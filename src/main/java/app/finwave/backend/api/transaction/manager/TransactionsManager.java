package app.finwave.backend.api.transaction.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.transaction.TransactionDatabase;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.api.transaction.hook.TransactionActionsHook;
import app.finwave.backend.api.transaction.hook.accumulation.AccumulationHook;
import app.finwave.backend.api.transaction.hook.accumulation.DefaultHook;
import app.finwave.backend.api.transaction.hook.accumulation.InternalHook;
import app.finwave.backend.api.transaction.manager.actions.*;
import app.finwave.backend.api.transaction.manager.data.TransactionEntry;
import app.finwave.backend.api.transaction.manager.records.BulkTransactionsRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewInternalRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.api.transaction.metadata.MetadataType;
import app.finwave.backend.database.DatabaseWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS_METADATA;

@Singleton
public class TransactionsManager {
    protected DSLContext context;
    protected DatabaseWorker databaseWorker;
    protected TransactionDatabase transactionDatabase;

    protected DefaultActionsWorker defaultActionsWorker;
    protected InternalActionsWorker internalActionsWorker;
    protected RecurringActionsWorker recurringActionsWorker;
    protected AccumulationActionsWorker accumulationActionsWorker;

    protected HashMap<MetadataType, TransactionActionsWorker<?,?,?>> actionsWorkers = new HashMap<>();

    protected ReentrantLock transactionLock = new ReentrantLock();

    @Inject
    public TransactionsManager(DatabaseWorker databaseWorker) {
        this.context = databaseWorker.getDefaultContext();
        this.databaseWorker = databaseWorker;
        this.transactionDatabase = databaseWorker.get(TransactionDatabase.class);

        this.defaultActionsWorker = new DefaultActionsWorker(databaseWorker);
        this.internalActionsWorker = new InternalActionsWorker(defaultActionsWorker, databaseWorker);
        this.recurringActionsWorker = new RecurringActionsWorker(databaseWorker);
        this.accumulationActionsWorker = new AccumulationActionsWorker(databaseWorker);

        actionsWorkers.put(MetadataType.WITHOUT_METADATA, defaultActionsWorker);
        actionsWorkers.put(MetadataType.INTERNAL_TRANSFER, internalActionsWorker);
        actionsWorkers.put(MetadataType.RECURRING, recurringActionsWorker);
        actionsWorkers.put(MetadataType.HAS_ACCUMULATION, accumulationActionsWorker);

        this.defaultActionsWorker.addHook(new DefaultHook(this, databaseWorker));
        this.accumulationActionsWorker.addHook(new AccumulationHook(this, databaseWorker));
        this.internalActionsWorker.addHook(new InternalHook(this, databaseWorker));
    }

    public void applyBulkTransactions(BulkTransactionsRecord record, int userId) {
        List<?> records = record.toRecords(userId);

        transactionLock.lock();

        try {
            context.transaction((configuration) -> {
                DSLContext dsl = configuration.dsl();
                var hooksInternal = internalActionsWorker.getHooks();
                var hooksDefault = defaultActionsWorker.getHooks();

                for (Object rawRecord : records) {
                    if (rawRecord instanceof TransactionNewRecord newRecord) {
                        hooksDefault.forEach((h) -> h.apply(dsl, newRecord));
                        long id = defaultActionsWorker.apply(dsl, newRecord);
                        hooksDefault.forEach((h) -> h.applied(dsl, newRecord, id));
                    }

                    if (rawRecord instanceof TransactionNewInternalRecord newRecord) {
                        hooksInternal.forEach((h) -> h.apply(dsl, newRecord));
                        long id = internalActionsWorker.apply(dsl, newRecord);
                        hooksInternal.forEach((h) -> h.applied(dsl, newRecord, id));
                    }
                }
            });
        }finally {
            transactionLock.unlock();
        }
    }

    public long applyInternalTransfer(TransactionNewInternalRecord newRecord) {
        transactionLock.lock();

        try {
            return context.transactionResult((configuration) -> {
                DSLContext dsl = configuration.dsl();
                var hooks = internalActionsWorker.getHooks();

                hooks.forEach((h) -> h.apply(dsl, newRecord));
                long id = internalActionsWorker.apply(dsl, newRecord);
                hooks.forEach((h) -> h.applied(dsl, newRecord, id));

                return id;
            });
        }finally {
            transactionLock.unlock();
        }
    }

    public long applyTransaction(TransactionNewRecord newRecord) {
        transactionLock.lock();

        try {
            return context.transactionResult((configuration) -> {
                DSLContext dsl = configuration.dsl();
                var hooks = defaultActionsWorker.getHooks();

                hooks.forEach((h) -> h.apply(dsl, newRecord));
                long id = defaultActionsWorker.apply(dsl, newRecord);
                hooks.forEach((h) -> h.applied(dsl, newRecord, id));

                return id;
            });
        }finally {
            transactionLock.unlock();
        }
    }

    public long applyRecurringTransaction(TransactionNewRecord newRecord) {
        transactionLock.lock();

        try {
            return context.transactionResult((configuration) -> {
                DSLContext dsl = configuration.dsl();
                var hooks = recurringActionsWorker.getHooks();

                hooks.forEach((h) -> h.apply(dsl, newRecord));
                long id = recurringActionsWorker.apply(configuration.dsl(), newRecord);
                hooks.forEach((h) -> h.applied(dsl, newRecord, id));

                return id;
            });
        }finally {
            transactionLock.unlock();
        }
    }

    public void editTransaction(long transactionId, TransactionEditRecord editRecord) {
        runTransactionOverRecord(transactionId, (context, record, type) -> {
            switch (type) {
                case WITHOUT_METADATA -> {
                    var hooks = defaultActionsWorker.getHooks();

                    hooks.forEach((h) -> h.edit(context, record, editRecord, transactionId));
                    defaultActionsWorker.edit(context, record, editRecord);
                    hooks.forEach((h) -> h.edited(context, record, editRecord, transactionId));
                }
                case INTERNAL_TRANSFER  -> {
                    var hooks = internalActionsWorker.getHooks();

                    hooks.forEach((h) -> h.edit(context, record, editRecord, transactionId));
                    internalActionsWorker.edit(context, record, editRecord);
                    hooks.forEach((h) -> h.edited(context, record, editRecord, transactionId));
                }
                case RECURRING -> {
                    var hooks = recurringActionsWorker.getHooks();

                    hooks.forEach((h) -> h.edit(context, record, editRecord, transactionId));
                    recurringActionsWorker.edit(context, record, editRecord);
                    hooks.forEach((h) -> h.edited(context, record, editRecord, transactionId));
                }
                case HAS_ACCUMULATION -> {
                    var hooks = accumulationActionsWorker.getHooks();

                    hooks.forEach((h) -> h.edit(context, record, editRecord, transactionId));
                    accumulationActionsWorker.edit(context, record, editRecord);
                    hooks.forEach((h) -> h.edited(context, record, editRecord, transactionId));
                }
            }
        });
    }

    public void cancelTransaction(long transactionId) {
        runTransactionOverRecord(transactionId, (context, record, type) -> {
            switch (type) {
                case WITHOUT_METADATA -> {
                    var hooks = defaultActionsWorker.getHooks();

                    hooks.forEach((h) -> h.cancel(context, record, transactionId));
                    defaultActionsWorker.cancel(context, record);
                    hooks.forEach((h) -> h.canceled(context, record, transactionId));
                }
                case INTERNAL_TRANSFER -> {
                    var hooks = internalActionsWorker.getHooks();

                    hooks.forEach((h) -> h.cancel(context, record, transactionId));
                    internalActionsWorker.cancel(context, record);
                    hooks.forEach((h) -> h.canceled(context, record, transactionId));
                }
                case RECURRING -> {
                    var hooks = recurringActionsWorker.getHooks();

                    hooks.forEach((h) -> h.cancel(context, record, transactionId));
                    recurringActionsWorker.cancel(context, record);
                    hooks.forEach((h) -> h.canceled(context, record, transactionId));
                }
                case HAS_ACCUMULATION -> {
                    var hooks = accumulationActionsWorker.getHooks();

                    hooks.forEach((h) -> h.cancel(context, record, transactionId));
                    accumulationActionsWorker.cancel(context, record);
                    hooks.forEach((h) -> h.canceled(context, record, transactionId));
                }
            }
        });
    }

    public DefaultActionsWorker getDefaultActionsWorker() {
        return defaultActionsWorker;
    }

    public InternalActionsWorker getInternalActionsWorker() {
        return internalActionsWorker;
    }

    public RecurringActionsWorker getRecurringActionsWorker() {
        return recurringActionsWorker;
    }

    public AccumulationActionsWorker getAccumulationActionsWorker() {
        return accumulationActionsWorker;
    }

    public List<TransactionEntry<?>> getTransactions(int userId, int offset, int count, TransactionsFilter filter) {
        List<Record> records = transactionDatabase.getTransactions(userId, offset, count, filter);

        ArrayList<TransactionEntry<?>> result = new ArrayList<>();
        HashMap<Long, TransactionEntry<?>> addedTransactions = new HashMap<>();

        for (Record record : records) {
            MetadataType metadataType = Optional.ofNullable(record.get(TRANSACTIONS_METADATA.TYPE))
                    .map(MetadataType::get)
                    .orElse(MetadataType.WITHOUT_METADATA);

            TransactionEntry<?> entry = actionsWorkers.get(metadataType).prepareEntry(context, record, addedTransactions);

            if (entry != null) {
                result.add(entry);
                addedTransactions.put(entry.transactionId, entry);
            }
        }

        return result;
    }

    public int getTransactionsCount(int userId, TransactionsFilter filter) {
        return transactionDatabase.getTransactionsCount(userId, filter);
    }

    public boolean userOwnTransaction(int userId, long transactionId) {
        return transactionDatabase.userOwnTransaction(userId, transactionId);
    }

    protected void runTransactionOverRecord(long transactionId, Transaction transaction) {
        transactionLock.lock();

        try {
            context.transaction((configuration) -> {
                DSLContext dsl = configuration.dsl();
                TransactionDatabase database = databaseWorker.get(TransactionDatabase.class, dsl);

                Record record = database
                        .getTransaction(transactionId)
                        .orElseThrow(() -> new RuntimeException("Transaction not exists"));

                MetadataType metadataType = Optional.ofNullable(record.get(TRANSACTIONS_METADATA.TYPE))
                        .map(MetadataType::get)
                        .orElse(MetadataType.WITHOUT_METADATA);

                transaction.run(dsl, record, metadataType);
            });
        }finally {
            transactionLock.unlock();
        }
    }

    interface Transaction {
        void run(DSLContext context, Record record, MetadataType type);
    }
}

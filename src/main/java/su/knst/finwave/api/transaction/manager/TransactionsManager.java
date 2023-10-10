package su.knst.finwave.api.transaction.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.api.transaction.manager.actions.TransactionActionsWorker;
import su.knst.finwave.api.transaction.manager.generator.TransactionEntry;
import su.knst.finwave.api.transaction.manager.actions.DefaultActionsWorker;
import su.knst.finwave.api.transaction.manager.actions.InternalActionsWorker;
import su.knst.finwave.api.transaction.manager.records.TransactionEditRecord;
import su.knst.finwave.api.transaction.manager.records.TransactionNewInternalRecord;
import su.knst.finwave.api.transaction.manager.records.TransactionNewRecord;
import su.knst.finwave.api.transaction.metadata.MetadataType;
import su.knst.finwave.database.DatabaseWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS_METADATA;

@Singleton
public class TransactionsManager {
    protected DSLContext context;
    protected DatabaseWorker databaseWorker;
    protected TransactionDatabase transactionDatabase;

    protected DefaultActionsWorker defaultActionsWorker = new DefaultActionsWorker();
    protected InternalActionsWorker internalActionsWorker = new InternalActionsWorker(defaultActionsWorker);
    protected HashMap<MetadataType, TransactionActionsWorker<?,?,?>> actionsWorkers = new HashMap<>();


    @Inject
    public TransactionsManager(DatabaseWorker databaseWorker) {
        this.context = databaseWorker.getDefaultContext();
        this.databaseWorker = databaseWorker;
        this.transactionDatabase = databaseWorker.get(TransactionDatabase.class);

        actionsWorkers.put(MetadataType.WITHOUT_METADATA, defaultActionsWorker);
        actionsWorkers.put(MetadataType.INTERNAL_TRANSFER, internalActionsWorker);
    }

    public long applyInternalTransfer(TransactionNewInternalRecord internalRecord) {
        return context.transactionResult((configuration) -> {
            DSLContext dsl = configuration.dsl();
            TransactionDatabase database = databaseWorker.get(TransactionDatabase.class, dsl);

            return internalActionsWorker.apply(dsl, database, internalRecord);
        });
    }

    public long applyTransaction(TransactionNewRecord newRecord) {
        return context.transactionResult((configuration) -> {
            DSLContext dsl = configuration.dsl();
            TransactionDatabase database = databaseWorker.get(TransactionDatabase.class, dsl);

            return defaultActionsWorker.apply(dsl, database, newRecord);
        });
    }

    public void editTransaction(long transactionId, TransactionEditRecord editRecord) {
        runTransactionOverRecord(transactionId, (context, record, type, database) -> {
            switch (type) {
                case WITHOUT_METADATA -> defaultActionsWorker.edit(context, database, record, editRecord);
                case INTERNAL_TRANSFER -> internalActionsWorker.edit(context, database, record, editRecord);
            }
        });
    }

    public void cancelTransaction(long transactionId) {
        runTransactionOverRecord(transactionId, (context, record, type, database) -> {
            switch (type) {
                case WITHOUT_METADATA -> defaultActionsWorker.cancel(context, database, record);
                case INTERNAL_TRANSFER -> internalActionsWorker.cancel(context, database, record);
            }
        });
    }

    public DefaultActionsWorker getDefaultActionsWorker() {
        return defaultActionsWorker;
    }

    public InternalActionsWorker getInternalActionsWorker() {
        return internalActionsWorker;
    }

    public List<TransactionEntry<?>> getTransactions(int userId, int offset, int count, TransactionsFilter filter) {
        List<Record> records = transactionDatabase.getTransactions(userId, offset, count, filter);

        ArrayList<TransactionEntry<?>> result = new ArrayList<>();
        HashMap<Long, TransactionEntry<?>> addedTransactions = new HashMap<>();

        for (Record record : records) {
            MetadataType metadataType = Optional.ofNullable(record.get(TRANSACTIONS_METADATA.TYPE))
                    .map(MetadataType::get)
                    .orElse(MetadataType.WITHOUT_METADATA);

            TransactionEntry<?> entry = actionsWorkers.get(metadataType).prepareEntry(context, transactionDatabase, record, addedTransactions);

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
        context.transaction((configuration) -> {
            DSLContext dsl = configuration.dsl();
            TransactionDatabase database = databaseWorker.get(TransactionDatabase.class, dsl);

            Record record = database
                    .getTransaction(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not exists"));

            MetadataType metadataType = Optional.ofNullable(record.get(TRANSACTIONS_METADATA.TYPE))
                    .map(MetadataType::get)
                    .orElse(MetadataType.WITHOUT_METADATA);

            transaction.run(dsl, record, metadataType, database);
        });
    }

    interface Transaction {
        void run(DSLContext context, Record record, MetadataType type, TransactionDatabase database);
    }
}

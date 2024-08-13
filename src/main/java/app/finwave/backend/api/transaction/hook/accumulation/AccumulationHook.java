package app.finwave.backend.api.transaction.hook.accumulation;

import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.accumulation.data.AccumulationData;
import app.finwave.backend.api.accumulation.AccumulationDatabase;
import app.finwave.backend.api.transaction.hook.TransactionActionsHook;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.api.transaction.metadata.MetadataDatabase;
import app.finwave.backend.database.DatabaseWorker;

import java.math.BigDecimal;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class AccumulationHook implements TransactionActionsHook<TransactionNewRecord, TransactionEditRecord> {
    protected TransactionsManager manager;
    protected DatabaseWorker databaseWorker;

    public AccumulationHook(TransactionsManager manager, DatabaseWorker databaseWorker) {
        this.manager = manager;
        this.databaseWorker = databaseWorker;
    }

    @Override
    public void apply(DSLContext context, TransactionNewRecord newRecord) {

    }

    @Override
    public void edit(DSLContext context, Record record, TransactionEditRecord editRecord, long transactionId) {

    }

    @Override
    public void cancel(DSLContext context, Record record, long transactionId) {

    }

    @Override
    public void applied(DSLContext context, TransactionNewRecord newRecord, long transactionId) {

    }

    @Override
    public void edited(DSLContext context, Record record, TransactionEditRecord editRecord, long transactionId) {
        AccumulationDatabase accumulationDatabase = databaseWorker.get(AccumulationDatabase.class, context);
        Optional<AccumulationData> optionalSettings = accumulationDatabase.getAccumulationSettings(record.get(TRANSACTIONS.ACCOUNT_ID));

        if (optionalSettings.isEmpty())
            return;

        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);

        AccumulationData data = optionalSettings.get();
        BigDecimal newRound = data.calculateRound(editRecord.delta().negate());

        long linkedTransactionId = record.get(TRANSACTIONS_METADATA.ARG);

        if (editRecord.delta().signum() > 0 || newRound.equals(BigDecimal.ZERO)) {
            manager.cancelTransaction(linkedTransactionId);

            context.update(TRANSACTIONS)
                    .set(TRANSACTIONS.METADATA_ID, (Long) null)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .execute();

            metadataDatabase.deleteMetadata(record.get(TRANSACTIONS.METADATA_ID));
        }else {
            editLinkedTransaction(context, newRound, linkedTransactionId);
        }

    }

    @Override
    public void canceled(DSLContext context, Record record, long transactionId) {
        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);
        long linkedTransactionId = record.get(TRANSACTIONS_METADATA.ARG);

        manager.cancelTransaction(linkedTransactionId);

        context.update(TRANSACTIONS)
                .set(TRANSACTIONS.METADATA_ID, (Long) null)
                .where(TRANSACTIONS.ID.eq(transactionId))
                .execute();

        metadataDatabase.deleteMetadata(record.get(TRANSACTIONS.METADATA_ID));
    }

    protected void editLinkedTransaction(DSLContext context, BigDecimal newDelta, long linkedTransactionId) {
        Record record = context.selectFrom(TRANSACTIONS
                        .leftJoin(TRANSACTIONS_METADATA)
                        .on(TRANSACTIONS.METADATA_ID.eq(TRANSACTIONS_METADATA.ID))
                        .leftJoin(INTERNAL_TRANSACTIONS_METADATA)
                        .on(TRANSACTIONS_METADATA.ARG.eq(INTERNAL_TRANSACTIONS_METADATA.ID))
                )
                .where(TRANSACTIONS.ID.eq(linkedTransactionId))
                .fetchOptional()
                .orElseThrow();

        long secondTransactionId = record.get(TRANSACTIONS.ID).equals(record.get(INTERNAL_TRANSACTIONS_METADATA.FROM_TRANSACTION_ID)) ?
                record.get(INTERNAL_TRANSACTIONS_METADATA.TO_TRANSACTION_ID) :
                record.get(INTERNAL_TRANSACTIONS_METADATA.FROM_TRANSACTION_ID);

        Record secondRecord = context.selectFrom(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(secondTransactionId))
                .fetchOptional()
                .orElseThrow();

        manager.editTransaction(linkedTransactionId, new TransactionEditRecord(
                record.get(TRANSACTIONS.CATEGORY_ID),
                record.get(TRANSACTIONS.ACCOUNT_ID),
                record.get(TRANSACTIONS.CREATED_AT),
                newDelta.negate(),
                record.get(TRANSACTIONS.DESCRIPTION)
        ));

        manager.editTransaction(secondTransactionId, new TransactionEditRecord(
                secondRecord.get(TRANSACTIONS.CATEGORY_ID),
                secondRecord.get(TRANSACTIONS.ACCOUNT_ID),
                secondRecord.get(TRANSACTIONS.CREATED_AT),
                newDelta,
                secondRecord.get(TRANSACTIONS.DESCRIPTION)
        ));
    }
}

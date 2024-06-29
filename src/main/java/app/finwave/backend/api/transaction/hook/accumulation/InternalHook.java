package app.finwave.backend.api.transaction.hook.accumulation;

import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.transaction.hook.TransactionActionsHook;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewInternalRecord;
import app.finwave.backend.api.transaction.metadata.MetadataDatabase;
import app.finwave.backend.api.transaction.metadata.MetadataType;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.Tables;
import app.finwave.backend.jooq.tables.records.InternalTransactionsMetadataRecord;

import java.util.Optional;

import static app.finwave.backend.jooq.Tables.INTERNAL_TRANSACTIONS_METADATA;
import static app.finwave.backend.jooq.Tables.TRANSACTIONS_METADATA;
import static app.finwave.backend.jooq.tables.Transactions.TRANSACTIONS;

public class InternalHook implements TransactionActionsHook<TransactionNewInternalRecord, TransactionEditRecord> {
    protected TransactionsManager manager;
    protected DatabaseWorker databaseWorker;

    public InternalHook(TransactionsManager manager, DatabaseWorker databaseWorker) {
        this.manager = manager;
        this.databaseWorker = databaseWorker;
    }

    @Override
    public void apply(DSLContext context, TransactionNewInternalRecord newRecord) {

    }

    @Override
    public void edit(DSLContext context, Record record, TransactionEditRecord editRecord, long transactionId) {

    }

    @Override
    public void cancel(DSLContext context, Record record, long transactionId) {
        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);
        Optional<Record> accumulationTransaction;

        if (record.get(TRANSACTIONS.DELTA).signum() < 0) {
            accumulationTransaction = fetchAccumulationTransaction(context, transactionId);
        }else {
            InternalTransactionsMetadataRecord metadataRecord = metadataDatabase
                    .getInternalMetadata(record.get(TRANSACTIONS_METADATA.ARG))
                    .orElseThrow();

            accumulationTransaction = fetchAccumulationTransaction(context, metadataRecord.getFromTransactionId());
        }

        if (accumulationTransaction.isEmpty())
            return;

        context.update(TRANSACTIONS)
                .set(TRANSACTIONS.METADATA_ID, (Long) null)
                .where(TRANSACTIONS.ID.eq(accumulationTransaction.get().get(TRANSACTIONS.ID)))
                .execute();

        metadataDatabase.deleteMetadata(accumulationTransaction.get().get(TRANSACTIONS.METADATA_ID));
    }

    @Override
    public void applied(DSLContext context, TransactionNewInternalRecord newRecord, long transactionId) {

    }

    @Override
    public void edited(DSLContext context, Record record, TransactionEditRecord editRecord, long transactionId) {

    }

    @Override
    public void canceled(DSLContext context, Record record, long transactionId) {

    }

    protected Optional<Record> fetchAccumulationTransaction(DSLContext context, long internalFromTransaction) {
        return context.selectFrom(TRANSACTIONS
                        .leftJoin(TRANSACTIONS_METADATA)
                        .on(Tables.TRANSACTIONS.METADATA_ID.eq(TRANSACTIONS_METADATA.ID))
                )
                .where(TRANSACTIONS_METADATA.TYPE.eq(MetadataType.HAS_ACCUMULATION.type)
                        .and(TRANSACTIONS_METADATA.ARG.eq(internalFromTransaction))
                )
                .fetchOptional();
    }

    protected long getSecondTransactionId(Record record, InternalTransactionsMetadataRecord metadataRecord) {
        return record.get(Tables.TRANSACTIONS.ID).equals(metadataRecord.getFromTransactionId()) ?
                metadataRecord.getToTransactionId() :
                metadataRecord.getFromTransactionId();
    }
}

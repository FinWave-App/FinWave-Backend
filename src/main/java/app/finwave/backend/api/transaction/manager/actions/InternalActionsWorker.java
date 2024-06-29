package app.finwave.backend.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.transaction.manager.data.InternalTransferMetadata;
import app.finwave.backend.api.transaction.manager.data.TransactionEntry;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewInternalRecord;
import app.finwave.backend.api.transaction.metadata.MetadataDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.InternalTransactionsMetadataRecord;
import app.finwave.backend.jooq.tables.records.TransactionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;

import java.util.HashMap;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class InternalActionsWorker extends TransactionActionsWorker<TransactionNewInternalRecord, TransactionEditRecord, InternalTransferMetadata> {
    protected DefaultActionsWorker defaultActionsWorker;

    public InternalActionsWorker(DefaultActionsWorker defaultActionsWorker, DatabaseWorker databaseWorker) {
        super(databaseWorker);
        this.defaultActionsWorker = defaultActionsWorker;
    }

    @Override
    public long apply(DSLContext context, TransactionNewInternalRecord newRecord) {
        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);

        long fromTransaction = defaultActionsWorker.apply(context, newRecord.from());
        long toTransaction = defaultActionsWorker.apply(context, newRecord.to());
        long transactionMeta = metadataDatabase.createInternalMetadata(fromTransaction, toTransaction);

        context.update(TRANSACTIONS)
                .set(TRANSACTIONS.METADATA_ID, transactionMeta)
                .where(TRANSACTIONS.ID.eq(fromTransaction).or(TRANSACTIONS.ID.eq(toTransaction)))
                .execute();

        return fromTransaction;
    }

    @Override
    public void edit(DSLContext context, Record record, TransactionEditRecord editRecord) {
        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);

        InternalTransactionsMetadataRecord metadataRecord = metadataDatabase
                .getInternalMetadata(record.get(TRANSACTIONS_METADATA.ARG))
                .orElseThrow();

        TransactionsRecord record2 = getSecondTransaction(record, metadataRecord, context)
                .orElseThrow(() -> new RuntimeException("Second transaction not exists"));

        if (record2.getAccountId().equals(editRecord.accountId()))
            throw new InvalidParameterException("accountId");

        if (editRecord.delta().signum() == 0 || record2.getDelta().signum() == editRecord.delta().signum())
            throw new InvalidParameterException("delta");

        defaultActionsWorker.edit(context, record, editRecord);
    }

    @Override
    public void cancel(DSLContext context, Record record) {
        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);

        InternalTransactionsMetadataRecord metadataRecord = metadataDatabase
                .getInternalMetadata(record.get(TRANSACTIONS_METADATA.ARG))
                .orElseThrow();

        TransactionsRecord record2 = getSecondTransaction(record, metadataRecord, context)
                .orElseThrow(() -> new RuntimeException("Second transaction not exists"));

        context.deleteFrom(INTERNAL_TRANSACTIONS_METADATA)
                .where(INTERNAL_TRANSACTIONS_METADATA.ID.eq(metadataRecord.getId()))
                .execute();

        defaultActionsWorker.cancel(context, record);
        defaultActionsWorker.cancel(context, record2);

        context.deleteFrom(TRANSACTIONS_METADATA)
                .where(TRANSACTIONS_METADATA.ID.eq(record.get(TRANSACTIONS_METADATA.ID)))
                .execute();
    }

    @Override
    public TransactionEntry<InternalTransferMetadata> prepareEntry(DSLContext context, Record record, HashMap<Long, TransactionEntry<?>> added) {
        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);

        long metadataId = record.get(TRANSACTIONS.METADATA_ID);
        boolean linkedInResult = added
                .values()
                .stream()
                .anyMatch((t) ->
                        t.metadata instanceof InternalTransferMetadata meta &&
                                meta.id == metadataId);

        if (linkedInResult)
            return null;

        InternalTransactionsMetadataRecord metadataRecord = metadataDatabase
                .getInternalMetadata(record.get(TRANSACTIONS_METADATA.ARG))
                .orElseThrow();

        TransactionEntry<?> secondTransaction = getSecondTransaction(record, metadataRecord, context).map(TransactionEntry::new).orElseThrow();

        return new TransactionEntry<>(record, new InternalTransferMetadata(metadataId, secondTransaction));
    }

    protected long getSecondTransactionId(Record record, InternalTransactionsMetadataRecord metadataRecord) {
        return record.get(TRANSACTIONS.ID).equals(metadataRecord.getFromTransactionId()) ?
                metadataRecord.getToTransactionId() :
                metadataRecord.getFromTransactionId();
    }

    protected Optional<TransactionsRecord> getSecondTransaction(Record record, InternalTransactionsMetadataRecord metadataRecord, DSLContext context) {
        long toFetch = getSecondTransactionId(record, metadataRecord);

        return context.selectFrom(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(toFetch))
                .fetchOptional();
    }
}

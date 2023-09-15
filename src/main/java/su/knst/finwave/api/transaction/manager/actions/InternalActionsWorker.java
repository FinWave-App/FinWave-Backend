package su.knst.finwave.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.manager.generator.InternalTransferMetadata;
import su.knst.finwave.api.transaction.manager.generator.TransactionEntry;
import su.knst.finwave.api.transaction.manager.records.TransactionEditRecord;
import su.knst.finwave.api.transaction.manager.records.TransactionNewInternalRecord;
import su.knst.finwave.api.transaction.metadata.MetadataType;
import su.knst.finwave.jooq.tables.records.InternalTransfersRecord;
import su.knst.finwave.jooq.tables.records.TransactionsRecord;
import su.knst.finwave.utils.params.InvalidParameterException;

import java.util.HashMap;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.*;

public class InternalActionsWorker implements TransactionActionsWorker<TransactionNewInternalRecord, TransactionEditRecord, InternalTransferMetadata> {
    protected DefaultActionsWorker defaultActionsWorker;

    public InternalActionsWorker(DefaultActionsWorker defaultActionsWorker) {
        this.defaultActionsWorker = defaultActionsWorker;
    }

    @Override
    public long apply(DSLContext context, TransactionDatabase database, TransactionNewInternalRecord newRecord) {
        long fromTransaction = defaultActionsWorker.apply(context, database, newRecord.from());
        long toTransaction = defaultActionsWorker.apply(context, database, newRecord.to());

        long internalId = context.insertInto(INTERNAL_TRANSFERS)
                .set(INTERNAL_TRANSFERS.FROM_TRANSACTION_ID, fromTransaction)
                .set(INTERNAL_TRANSFERS.TO_TRANSACTION_ID, toTransaction)
                .returningResult(INTERNAL_TRANSFERS.ID)
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        long transactionMeta = context.insertInto(TRANSACTIONS_METADATA)
                .set(TRANSACTIONS_METADATA.TYPE, MetadataType.INTERNAL_TRANSFER.type)
                .set(TRANSACTIONS_METADATA.ARG, internalId)
                .returningResult(TRANSACTIONS_METADATA.ID)
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        context.update(TRANSACTIONS)
                .set(TRANSACTIONS.METADATA_ID, transactionMeta)
                .where(TRANSACTIONS.ID.eq(fromTransaction).or(TRANSACTIONS.ID.eq(toTransaction)))
                .execute();

        return fromTransaction;
    }

    @Override
    public void edit(DSLContext context, TransactionDatabase database, Record record, TransactionEditRecord newRecord) {
        InternalTransfersRecord internalTransfersRecord = context.selectFrom(INTERNAL_TRANSFERS)
                .where(INTERNAL_TRANSFERS.ID.eq(record.get(TRANSACTIONS_METADATA.ARG)))
                .fetchOptional()
                .orElseThrow();

        TransactionsRecord record2 = getSecondTransaction(record, internalTransfersRecord, context)
                .orElseThrow(() -> new RuntimeException("Second transaction not exists"));

        if (record2.getAccountId().equals(newRecord.accountId()))
            throw new InvalidParameterException("accountId");

        if (newRecord.delta().signum() == 0 || record2.getDelta().signum() == newRecord.delta().signum())
            throw new InvalidParameterException("delta");

        defaultActionsWorker.edit(context, database, record, newRecord);
    }

    @Override
    public void cancel(DSLContext context, TransactionDatabase database, Record record) {
        InternalTransfersRecord internalTransfersRecord = context.selectFrom(INTERNAL_TRANSFERS)
                .where(INTERNAL_TRANSFERS.ID.eq(record.get(TRANSACTIONS_METADATA.ARG)))
                .fetchOptional()
                .orElseThrow();

        TransactionsRecord record2 = getSecondTransaction(record, internalTransfersRecord, context)
                .orElseThrow(() -> new RuntimeException("Second transaction not exists"));

        context.deleteFrom(INTERNAL_TRANSFERS)
                .where(INTERNAL_TRANSFERS.ID.eq(internalTransfersRecord.getId()))
                .execute();

        defaultActionsWorker.cancel(context, database, record);
        defaultActionsWorker.cancel(context, database, record2);

        context.deleteFrom(TRANSACTIONS_METADATA)
                .where(TRANSACTIONS_METADATA.ID.eq(record.get(TRANSACTIONS_METADATA.ID)))
                .execute();
    }

    @Override
    public TransactionEntry<InternalTransferMetadata> prepareEntry(DSLContext context, TransactionDatabase database, Record record, HashMap<Long, TransactionEntry<?>> added) {
        long metadataId = record.get(TRANSACTIONS.METADATA_ID);
        boolean linkedInResult = added
                .values()
                .stream()
                .anyMatch((t) ->
                        t.metadata instanceof InternalTransferMetadata meta &&
                                meta.id == metadataId);

        if (linkedInResult)
            return null;

        InternalTransfersRecord metaRecord = context.selectFrom(INTERNAL_TRANSFERS)
                .where(INTERNAL_TRANSFERS.ID.eq(record.get(TRANSACTIONS_METADATA.ARG)))
                .fetchOptional()
                .orElseThrow();

        TransactionEntry<?> secondTransaction = getSecondTransaction(record, metaRecord, context).map(TransactionEntry::new).orElseThrow();

        return new TransactionEntry<>(record, new InternalTransferMetadata(metadataId, secondTransaction));
    }

    protected long getSecondTransactionId(Record record, InternalTransfersRecord internalTransfersRecord) {
        return record.get(TRANSACTIONS.ID).equals(internalTransfersRecord.getFromTransactionId()) ?
                internalTransfersRecord.getToTransactionId() :
                internalTransfersRecord.getFromTransactionId();
    }

    protected Optional<TransactionsRecord> getSecondTransaction(Record record, InternalTransfersRecord internalTransfersRecord, DSLContext context) {
        long toFetch = getSecondTransactionId(record, internalTransfersRecord);

        return context.selectFrom(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(toFetch))
                .fetchOptional();
    }
}

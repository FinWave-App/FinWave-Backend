package su.knst.finwave.api.transaction.hook.accumulation;

import org.jooq.DSLContext;
import org.jooq.Record;
import su.knst.finwave.api.accumulation.data.AccumulationData;
import su.knst.finwave.api.accumulation.AccumulationDatabase;
import su.knst.finwave.api.transaction.hook.TransactionActionsHook;
import su.knst.finwave.api.transaction.manager.TransactionsManager;
import su.knst.finwave.api.transaction.manager.records.TransactionEditRecord;
import su.knst.finwave.api.transaction.manager.records.TransactionNewInternalRecord;
import su.knst.finwave.api.transaction.manager.records.TransactionNewRecord;
import su.knst.finwave.api.transaction.metadata.MetadataDatabase;
import su.knst.finwave.api.transaction.metadata.MetadataType;
import su.knst.finwave.database.DatabaseWorker;

import java.math.BigDecimal;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS;

public class DefaultHook implements TransactionActionsHook<TransactionNewRecord, TransactionEditRecord> {
    protected TransactionsManager manager;
    protected DatabaseWorker databaseWorker;

    public DefaultHook(TransactionsManager manager, DatabaseWorker databaseWorker) {
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
        if (newRecord.delta().signum() >= 0)
            return;

        AccumulationDatabase accumulationDatabase = databaseWorker.get(AccumulationDatabase.class, context);
        Optional<AccumulationData> optionalSettings = accumulationDatabase.getAccumulationSettings(newRecord.accountId());

        if (optionalSettings.isEmpty())
            return;

        AccumulationData data = optionalSettings.get();
        BigDecimal accumulationDelta = data.calculateRound(newRecord.delta().negate());

        if (accumulationDelta.equals(BigDecimal.ZERO))
            return;

        long internalTransferId = manager.applyInternalTransfer(new TransactionNewInternalRecord(
                data.ownerId(),
                data.tagId(),
                data.sourceAccountId(),
                data.targetAccountId(),
                newRecord.created(),
                accumulationDelta.negate(),
                accumulationDelta,
                null
        ));

        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);

        long metadataId = metadataDatabase.createMetadata(MetadataType.HAS_ACCUMULATION, internalTransferId);
        context.update(TRANSACTIONS)
                .set(TRANSACTIONS.METADATA_ID, metadataId)
                .where(TRANSACTIONS.ID.eq(transactionId))
                .execute();
    }

    @Override
    public void edited(DSLContext context, Record record, TransactionEditRecord editRecord, long transactionId) {
        if (editRecord.delta().signum() >= 0)
            return;

        applied(context, new TransactionNewRecord(
                record.get(TRANSACTIONS.OWNER_ID),
                editRecord.tagId(),
                editRecord.accountId(),
                editRecord.created(),
                editRecord.delta(),
                editRecord.description()
        ), transactionId);
    }

    @Override
    public void canceled(DSLContext context, Record record, long transactionId) {

    }
}

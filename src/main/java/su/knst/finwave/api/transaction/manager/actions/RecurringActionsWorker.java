package su.knst.finwave.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import su.knst.finwave.api.transaction.manager.generator.AbstractMetadata;
import su.knst.finwave.api.transaction.manager.generator.RecurringMetadata;
import su.knst.finwave.api.transaction.manager.generator.TransactionEntry;
import su.knst.finwave.api.transaction.manager.records.TransactionNewRecord;
import su.knst.finwave.api.transaction.metadata.MetadataDatabase;
import su.knst.finwave.api.transaction.metadata.MetadataType;
import su.knst.finwave.database.DatabaseWorker;

import java.util.HashMap;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS;

public class RecurringActionsWorker extends DefaultActionsWorker {
    public RecurringActionsWorker(DatabaseWorker databaseWorker) {
        super(databaseWorker);
    }

    @Override
    public long apply(DSLContext context, TransactionNewRecord newRecord) {
        MetadataDatabase metadataDatabase = databaseWorker.get(MetadataDatabase.class, context);

        long transactionId = super.apply(context, newRecord);
        long metadataId = metadataDatabase.createMetadata(MetadataType.RECURRING, 1);

        context.update(TRANSACTIONS)
                .set(TRANSACTIONS.METADATA_ID, metadataId)
                .where(TRANSACTIONS.ID.eq(transactionId))
                .execute();

        return transactionId;
    }

    @Override
    public TransactionEntry<AbstractMetadata> prepareEntry(DSLContext context, Record record, HashMap<Long, TransactionEntry<?>> added) {
        return new TransactionEntry<>(record, new RecurringMetadata());
    }
}

package app.finwave.backend.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.transaction.manager.data.AbstractMetadata;
import app.finwave.backend.api.transaction.manager.data.RecurringMetadata;
import app.finwave.backend.api.transaction.manager.data.TransactionEntry;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.api.transaction.metadata.MetadataDatabase;
import app.finwave.backend.api.transaction.metadata.MetadataType;
import app.finwave.backend.database.DatabaseWorker;

import java.util.HashMap;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

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

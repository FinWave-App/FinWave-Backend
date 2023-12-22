package su.knst.finwave.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import su.knst.finwave.api.transaction.manager.data.AbstractMetadata;
import su.knst.finwave.api.transaction.manager.data.AccumulationMetadata;
import su.knst.finwave.api.transaction.manager.data.TransactionEntry;
import su.knst.finwave.api.transaction.manager.records.TransactionEditRecord;
import su.knst.finwave.api.transaction.metadata.MetadataDatabase;
import su.knst.finwave.database.DatabaseWorker;

import java.util.HashMap;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS;
import static su.knst.finwave.jooq.Tables.TRANSACTIONS_METADATA;

public class AccumulationActionsWorker extends DefaultActionsWorker {
    public AccumulationActionsWorker(DatabaseWorker databaseWorker) {
        super(databaseWorker);
    }

    @Override
    public TransactionEntry<AbstractMetadata> prepareEntry(DSLContext context, Record record, HashMap<Long, TransactionEntry<?>> added) {
        return new TransactionEntry<>(record, new AccumulationMetadata(record.get(TRANSACTIONS_METADATA.ARG)));
    }
}

package app.finwave.backend.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.transaction.manager.data.AbstractMetadata;
import app.finwave.backend.api.transaction.manager.data.AccumulationMetadata;
import app.finwave.backend.api.transaction.manager.data.TransactionEntry;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.metadata.MetadataDatabase;
import app.finwave.backend.database.DatabaseWorker;

import java.util.HashMap;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;
import static app.finwave.backend.jooq.Tables.TRANSACTIONS_METADATA;

public class AccumulationActionsWorker extends DefaultActionsWorker {
    public AccumulationActionsWorker(DatabaseWorker databaseWorker) {
        super(databaseWorker);
    }

    @Override
    public TransactionEntry<AbstractMetadata> prepareEntry(DSLContext context, Record record, HashMap<Long, TransactionEntry<?>> added) {
        return new TransactionEntry<>(record, new AccumulationMetadata(record.get(TRANSACTIONS_METADATA.ARG)));
    }
}

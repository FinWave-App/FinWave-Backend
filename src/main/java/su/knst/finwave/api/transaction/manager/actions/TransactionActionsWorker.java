package su.knst.finwave.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.manager.generator.AbstractMetadata;
import su.knst.finwave.api.transaction.manager.generator.TransactionEntry;
import su.knst.finwave.database.DatabaseWorker;

import java.util.HashMap;

public abstract class TransactionActionsWorker<T, Y, Z extends AbstractMetadata> {
    protected DatabaseWorker databaseWorker;

    public TransactionActionsWorker(DatabaseWorker databaseWorker) {
        this.databaseWorker = databaseWorker;
    }

    public abstract long apply(DSLContext context, T newRecord);
    public abstract void edit(DSLContext context, Record record, Y editRecord);
    public abstract void cancel(DSLContext context, Record record);
    public abstract TransactionEntry<Z> prepareEntry(DSLContext context, Record record, HashMap<Long, TransactionEntry<?>> added);
}

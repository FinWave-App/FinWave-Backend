package app.finwave.backend.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.transaction.manager.data.AbstractMetadata;
import app.finwave.backend.api.transaction.manager.data.TransactionEntry;
import app.finwave.backend.api.transaction.hook.TransactionActionsHook;
import app.finwave.backend.database.DatabaseWorker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class TransactionActionsWorker<T, Y, Z extends AbstractMetadata> {
    protected DatabaseWorker databaseWorker;
    protected ArrayList<TransactionActionsHook<T, Y>> hooks = new ArrayList<>();

    public TransactionActionsWorker(DatabaseWorker databaseWorker) {
        this.databaseWorker = databaseWorker;
    }

    public abstract long apply(DSLContext context, T newRecord);
    public abstract void edit(DSLContext context, Record record, Y editRecord);
    public abstract void cancel(DSLContext context, Record record);
    public abstract TransactionEntry<Z> prepareEntry(DSLContext context, Record record, HashMap<Long, TransactionEntry<?>> added);

    public void addHook(TransactionActionsHook<T, Y> hook) {
        hooks.add(hook);
    }

    public void removeHook(TransactionActionsHook<T, Y> hook) {
        hooks.remove(hook);
    }

    public List<TransactionActionsHook<T, Y>> getHooks() {
        return Collections.unmodifiableList(hooks);
    }
}

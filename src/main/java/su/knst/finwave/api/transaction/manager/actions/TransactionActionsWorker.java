package su.knst.finwave.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.manager.generator.AbstractMetadata;
import su.knst.finwave.api.transaction.manager.generator.TransactionEntry;

import java.util.HashMap;

public interface TransactionActionsWorker<T, Y, Z extends AbstractMetadata> {
    long apply(DSLContext context, TransactionDatabase database, T newRecord);
    void edit(DSLContext context, TransactionDatabase database, Record record, Y editRecord);
    void cancel(DSLContext context, TransactionDatabase database, Record record);
    TransactionEntry<Z> prepareEntry(DSLContext context, TransactionDatabase database, Record record, HashMap<Long, TransactionEntry<?>> added);
}

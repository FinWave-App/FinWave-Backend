package app.finwave.backend.api.transaction.hook;

import org.jooq.DSLContext;
import org.jooq.Record;
import app.finwave.backend.api.transaction.manager.data.AbstractMetadata;

public interface TransactionActionsHook<T, Y> {
    void apply(DSLContext context, T newRecord);
    void edit(DSLContext context, Record record, Y editRecord, long transactionId);
    void cancel(DSLContext context, Record record, long transactionId);

    void applied(DSLContext context, T newRecord, long transactionId);
    void edited(DSLContext context, Record record, Y editRecord, long transactionId);
    void canceled(DSLContext context, Record record, long transactionId);
}

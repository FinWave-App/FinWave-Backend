package su.knst.finwave.api.transaction.generator;

import org.jooq.Record;

import java.util.HashMap;

public interface PrepareHandler {
    TransactionEntry<?> prepare(Record record, HashMap<Long, TransactionEntry<?>> added);
}

package su.knst.finwave.api.transaction.generator;

import org.jooq.Record;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS;

public class TransactionEntry<T extends AbstractMetadata> {
    public final long transactionId;
    public final long tagId;
    public final long accountId;
    public final long currencyId;
    public final OffsetDateTime createdAt;
    public final BigDecimal delta;
    public final String description;
    public final T metadata;

    public TransactionEntry(long transactionId, long tagId, long accountId, long currencyId, OffsetDateTime createdAt, BigDecimal delta, String description, T metadata) {
        this.transactionId = transactionId;
        this.tagId = tagId;
        this.accountId = accountId;
        this.currencyId = currencyId;
        this.createdAt = createdAt;
        this.delta = delta;
        this.description = description;
        this.metadata = metadata;
    }

    public TransactionEntry(long transactionId, long tagId, long accountId, long currencyId, OffsetDateTime createdAt, BigDecimal delta, String description) {
        this(transactionId, tagId, accountId, currencyId, createdAt, delta, description, null);
    }

    public TransactionEntry(Record record, T metadata) {
        this(
                record.get(TRANSACTIONS.ID),
                record.get(TRANSACTIONS.TAG_ID),
                record.get(TRANSACTIONS.ACCOUNT_ID),
                record.get(TRANSACTIONS.CURRENCY_ID),
                record.get(TRANSACTIONS.CREATED_AT),
                record.get(TRANSACTIONS.DELTA),
                record.get(TRANSACTIONS.DESCRIPTION),
                metadata
        );
    }

    public TransactionEntry(Record record) {
        this(record, null);
    }
}

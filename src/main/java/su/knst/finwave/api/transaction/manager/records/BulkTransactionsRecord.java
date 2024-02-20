package su.knst.finwave.api.transaction.manager.records;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record BulkTransactionsRecord(List<Entry> entries) {
    public List<?> toRecords(int userId) {
        return entries.stream().map((e) -> {
            if (e.type == 1)
                return new TransactionNewInternalRecord(userId, e.tagId, e.accountId, e.toAccountId, e.created, e.delta, e.toDelta, e.description);

            return new TransactionNewRecord(userId, e.tagId, e.accountId, e.created, e.delta, e.description);
        }).collect(Collectors.toList());
    }

    public static class Entry {
        public final short type; // 0 - default transaction, 1 - internal
        public final long tagId;
        public final long accountId;
        public final OffsetDateTime created;
        public final BigDecimal delta;
        public final String description;


        // for internal records
        public long toAccountId;
        public BigDecimal toDelta;

        public Entry(short type, long tagId, long accountId, OffsetDateTime created, BigDecimal delta, String description, long toAccountId, BigDecimal toDelta) {
            this.type = type;
            this.tagId = tagId;
            this.accountId = accountId;
            this.created = created;
            this.delta = delta;
            this.description = description;
            this.toAccountId = toAccountId;
            this.toDelta = toDelta;
        }
    }
}

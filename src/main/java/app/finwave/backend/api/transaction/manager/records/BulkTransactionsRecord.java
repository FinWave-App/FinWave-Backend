package app.finwave.backend.api.transaction.manager.records;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record BulkTransactionsRecord(List<Entry> entries) {
    public List<?> toRecords(int userId) {
        return entries.stream().map((e) -> {
            if (e.type == 1)
                return new TransactionNewInternalRecord(userId, e.categoryId, e.accountId, e.toAccountId, e.created, e.delta, e.toDelta, e.description);

            return new TransactionNewRecord(userId, e.categoryId, e.accountId, e.created, e.delta, e.description);
        }).collect(Collectors.toList());
    }

    public static class Entry {
        public short type; // 0 - default transaction, 1 - internal
        public long categoryId;
        public long accountId;
        public OffsetDateTime created;
        public BigDecimal delta;
        public String description;


        // for internal records
        public long toAccountId;
        public BigDecimal toDelta;

        public Entry(short type, long categoryId, long accountId, OffsetDateTime created, BigDecimal delta, String description, long toAccountId, BigDecimal toDelta) {
            this.type = type;
            this.categoryId = categoryId;
            this.accountId = accountId;
            this.created = created;
            this.delta = delta;
            this.description = description;
            this.toAccountId = toAccountId;
            this.toDelta = toDelta;
        }
    }
}

package app.finwave.backend.api.transaction.manager.records;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionNewInternalRecord(int userId, long tagId, long fromAccountId, long toAccountId, OffsetDateTime created, BigDecimal fromDelta, BigDecimal toDelta, String description) {
    public TransactionNewRecord from() {
        return new TransactionNewRecord(userId, tagId, fromAccountId, created, fromDelta, description);
    }

    public TransactionNewRecord to() {
        return new TransactionNewRecord(userId, tagId, toAccountId, created, toDelta, description);
    }
}

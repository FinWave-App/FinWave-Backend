package app.finwave.backend.api.transaction.manager.records;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionNewInternalRecord(int userId, long categoryId, long fromAccountId, long toAccountId, OffsetDateTime created, BigDecimal fromDelta, BigDecimal toDelta, String description) {
    public TransactionNewRecord from() {
        return new TransactionNewRecord(userId, categoryId, fromAccountId, created, fromDelta, description);
    }

    public TransactionNewRecord to() {
        return new TransactionNewRecord(userId, categoryId, toAccountId, created, toDelta, description);
    }
}

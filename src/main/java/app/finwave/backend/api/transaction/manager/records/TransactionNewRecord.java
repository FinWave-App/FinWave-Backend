package app.finwave.backend.api.transaction.manager.records;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionNewRecord(int userId, long categoryId, long accountId, OffsetDateTime created, BigDecimal delta, String description) {
}

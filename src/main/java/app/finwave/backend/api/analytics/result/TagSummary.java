package app.finwave.backend.api.analytics.result;

import java.math.BigDecimal;

public record TagSummary(long currencyId, long tagId, BigDecimal delta) {
}

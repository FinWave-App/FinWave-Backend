package app.finwave.backend.api.analytics.result;

import java.math.BigDecimal;

public record CategorySummary(long currencyId, long categoryId, BigDecimal delta) {
}

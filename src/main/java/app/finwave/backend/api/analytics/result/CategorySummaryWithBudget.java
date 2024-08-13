package app.finwave.backend.api.analytics.result;

import java.math.BigDecimal;

public record CategorySummaryWithBudget(long currencyId, long categoryId, long budgetId, BigDecimal delta) {
}

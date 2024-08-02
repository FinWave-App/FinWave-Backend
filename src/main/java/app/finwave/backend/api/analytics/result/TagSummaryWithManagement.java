package app.finwave.backend.api.analytics.result;

import java.math.BigDecimal;

public record TagSummaryWithManagement(long currencyId, long tagId, long managementId, BigDecimal delta) {
}

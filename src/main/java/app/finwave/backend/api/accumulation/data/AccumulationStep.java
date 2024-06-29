package app.finwave.backend.api.accumulation.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AccumulationStep(BigDecimal from, BigDecimal to, BigDecimal step) {
    public BigDecimal calculateRound(BigDecimal amount) {
        if (from != null && amount.compareTo(from) < 0 || to != null && amount.compareTo(to) > 0 || step == null)
            return BigDecimal.ZERO;

        return amount.divide(step, 0, RoundingMode.CEILING).multiply(step).subtract(amount);
    }
}

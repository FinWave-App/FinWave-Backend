package su.knst.finwave.service.recurring;

import su.knst.finwave.api.transaction.recurring.RepeatType;

import java.time.OffsetDateTime;

public class NextRepeatCalculator {
    public static OffsetDateTime calculate(OffsetDateTime lastRepeat, short type, short arg) {
        RepeatType repeatType = RepeatType.values()[type];

        switch (repeatType) {
            case DAILY -> {
                return lastRepeat.plusDays(1);
            }
            case IN_DAYS -> {
                return lastRepeat.plusDays(arg);
            }
            case WEEKLY -> {
                return lastRepeat.plusWeeks(1);
            }
            case MONTHLY -> {
                return lastRepeat.plusMonths(1);
            }
        }

        return lastRepeat.plusMonths(1);
    }
}

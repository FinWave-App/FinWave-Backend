package su.knst.finwave.service.recurring;

import su.knst.finwave.api.transaction.recurring.RepeatType;

import java.time.OffsetDateTime;

public class NextRepeatTools {
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

    public static boolean validateArg(RepeatType type, short arg) {
        if (type == RepeatType.IN_DAYS)
            return arg > 0 && arg < 512;

        return true;
    }

    public static boolean validateArg(short type, short arg) {
        RepeatType[] values = RepeatType.values();

        if (type < 0 || type >= values.length)
            return false;

        return validateArg(values[type], arg);
    }
}

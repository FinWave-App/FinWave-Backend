package app.finwave.backend.service.recurring;

import app.finwave.backend.api.recurring.RepeatType;

import java.time.OffsetDateTime;

public class NextRepeatTools {
    public static OffsetDateTime calculate(OffsetDateTime lastRepeat, short type, short arg) {
        RepeatType repeatType = RepeatType.values()[type];

        switch (repeatType) {
            case IN_DAYS -> {
                return lastRepeat.plusDays(arg);
            }
            case WEEKLY -> {
                return lastRepeat.plusWeeks(arg);
            }
            case MONTHLY -> {
                return lastRepeat.plusMonths(arg);
            }
        }

        return lastRepeat.plusMonths(1);
    }

    public static boolean validateArg(RepeatType type, short arg) {
        return arg > 0 && arg <= 512;
    }

    public static boolean validateArg(short type, short arg) {
        RepeatType[] values = RepeatType.values();

        if (type < 0 || type >= values.length)
            return false;

        return validateArg(values[type], arg);
    }
}

package su.knst.fintrack.utils.params;

import spark.Request;
import su.knst.fintrack.utils.params.validators.IntValidator;
import su.knst.fintrack.utils.params.validators.LongValidator;
import su.knst.fintrack.utils.params.validators.StringValidator;

public class ParamsValidator {
    public static StringValidator string(String raw) {
        return new StringValidator(raw);
    }

    public static StringValidator string(Request request, String name) {
        return string(request.queryParams(name));
    }

    public static IntValidator integer(String raw) {
        Integer number = null;

        if (raw != null)
            try {
                number = Integer.parseInt(raw);
            } catch (Exception ignored) {
            }

        return new IntValidator(number);
    }

    public static IntValidator integer(Request request, String name) {
        return integer(request.queryParams(name));
    }

    public static LongValidator longV(String raw) {
        Long number = null;

        if (raw != null)
            try {
                number = Long.parseLong(raw);
            } catch (Exception ignored) {
            }

        return new LongValidator(number);
    }

    public static LongValidator longV(Request request, String name) {
        return longV(request.queryParams(name));
    }
}

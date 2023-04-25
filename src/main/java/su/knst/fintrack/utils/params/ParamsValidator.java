package su.knst.fintrack.utils.params;

import spark.Request;
import su.knst.fintrack.utils.params.validators.IntValidator;
import su.knst.fintrack.utils.params.validators.LongValidator;
import su.knst.fintrack.utils.params.validators.StringValidator;

public class ParamsValidator {
    public static StringValidator string(Request request, String name) {
        return new StringValidator(request.queryParams(name));
    }

    public static IntValidator integer(Request request, String name) {
        String value = request.queryParams(name);
        Integer number = null;

        if (value != null)
            try {
                number = Integer.parseInt(value);
            } catch (Exception ignored) {
            }

        return new IntValidator(number);
    }

    public static LongValidator longV(Request request, String name) {
        String value = request.queryParams(name);
        Long number = null;

        if (value != null)
            try {
                number = Long.parseLong(value);
            } catch (Exception ignored) {
            }

        return new LongValidator(number);
    }
}

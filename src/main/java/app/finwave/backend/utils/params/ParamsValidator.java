package app.finwave.backend.utils.params;

import com.google.gson.JsonSyntaxException;
import spark.Request;
import app.finwave.backend.utils.params.validators.BodyValidator;
import app.finwave.backend.utils.params.validators.IntValidator;
import app.finwave.backend.utils.params.validators.LongValidator;
import app.finwave.backend.utils.params.validators.StringValidator;

import static app.finwave.backend.api.ApiResponse.GSON;

public class ParamsValidator {
    public static StringValidator string(String raw, String name) {
        return new StringValidator(raw, name);
    }

    public static StringValidator string(String raw) {
        return string(raw, null);
    }

    public static StringValidator string(Request request, String name) {
        return string(request.queryParams(name), name);
    }

    public static IntValidator integer(String raw, String name) {
        Integer number = null;

        if (raw != null)
            try {
                number = Integer.parseInt(raw);
            } catch (Exception ignored) {
            }

        return new IntValidator(number, name);
    }

    public static IntValidator integer(String raw) {
        return integer(raw, null);
    }

    public static IntValidator integer(Request request, String name) {
        return integer(request.queryParams(name), name);
    }

    public static LongValidator longV(String raw, String name) {
        Long number = null;

        if (raw != null)
            try {
                number = Long.parseLong(raw);
            } catch (Exception ignored) {
            }

        return new LongValidator(number, name);
    }

    public static LongValidator longV(String raw) {
        return longV(raw, null);
    }

    public static LongValidator longV(Request request, String name) {
        return longV(request.queryParams(name), name);
    }

    public static <T> BodyValidator<T> bodyObject(Request request, Class<T> tClass) {
        try {
            return bodyObject(GSON.fromJson(request.body(), tClass));
        }catch (Exception e) {
            throw new InvalidParameterException("body");
        }
    }

    public static <T> BodyValidator<T> bodyObject(T object) {
        try {
            return new BodyValidator<>(object);
        }catch (JsonSyntaxException e) {
            throw new InvalidParameterException("body");
        }
    }
}

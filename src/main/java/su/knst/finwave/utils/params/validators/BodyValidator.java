package su.knst.finwave.utils.params.validators;

import java.util.function.Function;

public class BodyValidator<T> extends AbstractValidator<T> {
    public BodyValidator(T raw) {
        super(raw, null);
    }

    public BodyValidator<T> matches(Function<T, Boolean> validator, String name) {
        if (raw != null && !validator.apply(raw))
            invalid(name);

        return this;
    }

    public BodyValidator<T> matches(Function<T, Boolean> validator) {
        return matches(validator, name);
    }
}

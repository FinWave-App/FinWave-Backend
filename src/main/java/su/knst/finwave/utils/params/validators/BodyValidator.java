package su.knst.finwave.utils.params.validators;

import java.util.function.Function;

public class BodyValidator<T> extends AbstractValidator<T> {
    public BodyValidator(T raw) {
        super(raw, null);
    }

    public BodyValidator<T> matches(Function<T, Boolean> validator) {
        if (raw != null && !validator.apply(raw))
            invalid();

        return this;
    }
}

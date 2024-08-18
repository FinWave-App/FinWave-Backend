package app.finwave.backend.utils.params.validators;

import java.util.function.Function;

public class BodyValidator<T> extends AbstractValidator<T> {
    public BodyValidator(T raw) {
        super(raw, null);
    }

    public BodyValidator<T> matches(ValidatorFunc<T, Boolean> validator, String name) {
        try {
            if (raw != null && !validator.apply(raw))
                invalid(name);
        } catch (Exception e) {
            invalid();
        }

        return this;
    }

    public BodyValidator<T> matches(ValidatorFunc<T, Boolean> validator) {
        return matches(validator, name);
    }
}

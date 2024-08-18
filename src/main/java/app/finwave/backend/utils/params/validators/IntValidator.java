package app.finwave.backend.utils.params.validators;

import java.util.function.Function;

public class IntValidator extends AbstractValidator<Integer> {
    public IntValidator(Integer raw, String name) {
        super(raw, name);
    }

    public IntValidator(Integer raw) {
        super(raw, null);
    }

    public IntValidator range(int from, int to) {
        if (raw != null && (raw < from || raw > to))
            invalid();

        return this;
    }

    public IntValidator matches(ValidatorFunc<Integer, Boolean> validator) {
        try {
            if (raw != null && !validator.apply(raw))
                invalid();
        } catch (Exception e) {
            invalid();
        }

        return this;
    }
}

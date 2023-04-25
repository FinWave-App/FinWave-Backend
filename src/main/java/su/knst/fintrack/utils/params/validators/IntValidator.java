package su.knst.fintrack.utils.params.validators;

import java.util.function.Function;

public class IntValidator extends AbstractValidator<Integer> {
    public IntValidator(Integer raw) {
        super(raw);
    }

    public IntValidator range(int from, int to) {
        if (raw != null && (raw < from || raw > to))
            invalid();

        return this;
    }

    public IntValidator matches(Function<Integer, Boolean> validator) {
        if (raw != null && !validator.apply(raw))
            invalid();

        return this;
    }
}

package app.finwave.backend.utils.params.validators;

import java.util.function.Function;

public class LongValidator extends AbstractValidator<Long> {
    public LongValidator(Long raw, String name) {
        super(raw, name);
    }

    public LongValidator(Long raw) {
        super(raw, null);
    }

    public LongValidator range(long from, long to) {
        if (raw != null && (raw < from || raw > to))
            invalid();

        return this;
    }

    public LongValidator matches(Function<Long, Boolean> validator) {
        if (raw != null && !validator.apply(raw))
            invalid();

        return this;
    }
}

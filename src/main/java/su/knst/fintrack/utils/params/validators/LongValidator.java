package su.knst.fintrack.utils.params.validators;

import java.util.function.Function;

public class LongValidator extends AbstractValidator<Long> {
    public LongValidator(Long raw) {
        super(raw);
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

package su.knst.fintrack.utils.params.validators;

import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractValidator<T> {
    protected T raw;

    protected AbstractValidator(T raw) {
        this.raw = raw;
    }

    protected void invalid() {
        throw new IllegalArgumentException();
    }

    public Optional<T> optional() {
        return Optional.ofNullable(raw);
    }

    public T require() {
        Optional<T> result = optional();

        if (result.isEmpty())
            invalid();

        return raw;
    }

    public <X> X map(Function<T, X> mapper) {
        try {
            return mapper.apply(raw);
        } catch (Exception e) {
            invalid();

            return null;
        }
    }

    public <X> Optional<X> mapOptional(Function<T, X> mapper) {
        try {
            return Optional.ofNullable(mapper.apply(raw));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

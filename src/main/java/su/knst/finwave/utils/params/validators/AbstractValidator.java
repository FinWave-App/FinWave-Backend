package su.knst.finwave.utils.params.validators;

import su.knst.finwave.utils.params.InvalidParameterException;

import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractValidator<T> {
    protected T raw;
    protected String name;

    protected AbstractValidator(T raw, String name) {
        this.raw = raw;
        this.name = name;
    }

    protected void invalid() {
        invalid(name);
    }

    protected void invalid(String name) {
        throw name == null ? new InvalidParameterException() : new InvalidParameterException(name);
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

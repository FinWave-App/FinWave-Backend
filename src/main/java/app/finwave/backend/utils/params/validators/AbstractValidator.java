package app.finwave.backend.utils.params.validators;

import app.finwave.backend.utils.params.InvalidParameterException;

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

    public <X> X map(ValidatorFunc<T, X> mapper) {
        try {
            return mapper.apply(require());
        } catch (Exception e) {
            invalid();

            return null;
        }
    }

    public <X> Optional<X> mapOptional(ValidatorFunc<T, X> mapper) {
        try {
            return optional().map((x) -> {
                try {
                    return mapper.apply(x);
                } catch (Exception e) {
                    return null;
                }
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

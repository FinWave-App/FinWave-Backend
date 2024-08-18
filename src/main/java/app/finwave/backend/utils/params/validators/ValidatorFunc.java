package app.finwave.backend.utils.params.validators;

public interface ValidatorFunc<T, R> {
    R apply(T t) throws Exception;
}

package su.knst.fintrack.utils.params.validators;

import java.util.function.Function;

public class StringValidator extends AbstractValidator<String> {
    public StringValidator(String raw, String name) {
        super(raw, name);
    }

    public StringValidator(String raw) {
        super(raw, null);
    }

    public StringValidator length(int from, int to) {
        if (raw == null)
            return this;

        int length = raw.length();
        if (length < from || length > to) invalid();

        return this;
    }

    public StringValidator matches(Function<String, Boolean> validator) {
        if (raw != null && !validator.apply(raw))
            invalid();

        return this;
    }

    public StringValidator matches(String regex) {
        return matches((v) -> v.matches(regex));
    }

    public StringValidator contains(String text) {
        if (raw != null && !raw.contains(text))
            invalid();

        return this;
    }
}

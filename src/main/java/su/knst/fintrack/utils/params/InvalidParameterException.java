package su.knst.fintrack.utils.params;

public class InvalidParameterException extends IllegalArgumentException {
    public InvalidParameterException() {

    }

    public InvalidParameterException(String name) {
        super("'" + name + "' parameter invalid");
    }
}

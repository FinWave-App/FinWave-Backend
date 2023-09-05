package su.knst.finwave.api.transaction.generator;

public abstract class AbstractMetadata {
    public final long type;

    public AbstractMetadata(long type) {
        this.type = type;
    }
}

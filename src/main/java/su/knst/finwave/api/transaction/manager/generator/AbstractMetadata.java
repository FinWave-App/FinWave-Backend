package su.knst.finwave.api.transaction.manager.generator;

public abstract class AbstractMetadata {
    public final long type;

    public AbstractMetadata(long type) {
        this.type = type;
    }
}

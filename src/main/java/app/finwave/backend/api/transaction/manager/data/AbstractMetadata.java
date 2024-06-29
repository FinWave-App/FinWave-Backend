package app.finwave.backend.api.transaction.manager.data;

public abstract class AbstractMetadata {
    public final long type;

    public AbstractMetadata(long type) {
        this.type = type;
    }
}

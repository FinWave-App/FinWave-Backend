package app.finwave.backend.api.transaction.metadata;

public enum MetadataType {
    WITHOUT_METADATA(0),
    INTERNAL_TRANSFER(1),
    RECURRING(2),
    HAS_ACCUMULATION(3);

    public final short type;

    MetadataType(int type) {
        this.type = (short) type;
    }

    public static MetadataType get(short type) {
        return MetadataType.values()[type];
    }
}

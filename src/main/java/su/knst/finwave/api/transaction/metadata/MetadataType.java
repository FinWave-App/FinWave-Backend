package su.knst.finwave.api.transaction.metadata;

public enum MetadataType {
    INTERNAL_TRANSFER(1);

    public final short type;

    MetadataType(int type) {
        this.type = (short) type;
    }

    public static MetadataType get(short type) {
        return MetadataType.values()[type - 1];
    }
}

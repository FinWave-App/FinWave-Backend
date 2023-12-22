package su.knst.finwave.api.transaction.manager.data;

import su.knst.finwave.api.transaction.metadata.MetadataType;

public class AccumulationMetadata extends AbstractMetadata {
    public final long internalTransactionId;

    public AccumulationMetadata(long internalTransactionId) {
        super(MetadataType.HAS_ACCUMULATION.type);
        this.internalTransactionId = internalTransactionId;
    }
}

package app.finwave.backend.api.transaction.manager.data;

import app.finwave.backend.api.transaction.metadata.MetadataType;

public class AccumulationMetadata extends AbstractMetadata {
    public final long internalTransactionId;

    public AccumulationMetadata(long internalTransactionId) {
        super(MetadataType.HAS_ACCUMULATION.type);
        this.internalTransactionId = internalTransactionId;
    }
}

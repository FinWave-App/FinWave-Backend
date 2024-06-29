package app.finwave.backend.api.transaction.manager.data;

import app.finwave.backend.api.transaction.metadata.MetadataType;

public class InternalTransferMetadata extends AbstractMetadata {
    public final long id;
    public final TransactionEntry<?> linkedTransaction;

    public InternalTransferMetadata(long id, TransactionEntry<?> linkedTransaction) {
        super(MetadataType.INTERNAL_TRANSFER.type);

        this.id = id;
        this.linkedTransaction = linkedTransaction;
    }
}

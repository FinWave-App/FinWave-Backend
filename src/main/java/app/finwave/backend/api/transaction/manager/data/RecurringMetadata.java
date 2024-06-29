package app.finwave.backend.api.transaction.manager.data;

import app.finwave.backend.api.transaction.metadata.MetadataType;

public class RecurringMetadata extends AbstractMetadata {
    public RecurringMetadata() {
        super(MetadataType.RECURRING.type);
    }
}

package su.knst.finwave.api.transaction.manager.generator;

import su.knst.finwave.api.transaction.metadata.MetadataType;

public class RecurringMetadata extends AbstractMetadata {
    public RecurringMetadata() {
        super(MetadataType.RECURRING.type);
    }
}

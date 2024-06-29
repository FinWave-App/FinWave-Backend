package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class RecurringTransactionConfig implements GroupedConfig {
    public int maxDescriptionLength = 256;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

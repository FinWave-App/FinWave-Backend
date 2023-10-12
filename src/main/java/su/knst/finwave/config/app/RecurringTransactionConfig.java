package su.knst.finwave.config.app;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class RecurringTransactionConfig implements GroupedConfig {
    public int maxDescriptionLength = 256;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

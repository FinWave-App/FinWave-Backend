package su.knst.fintrack.config.app;

import su.knst.fintrack.config.ConfigGroup;
import su.knst.fintrack.config.GroupedConfig;

public class TransactionConfig implements GroupedConfig {

    public int maxTransactionsInListPerRequest = 128;
    public int maxDescriptionLength = 256;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

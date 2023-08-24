package su.knst.finwave.config.app;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class TransactionConfig implements GroupedConfig {
    public int maxTransactionsInListPerRequest = 128;
    public int maxDescriptionLength = 256;

    public TransactionTagConfig tags = new TransactionTagConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }

    public static class TransactionTagConfig {
        public int maxTagsPerUser = 256;
        public int maxNameLength = 64;
        public int maxDescriptionLength = 128;
    }
}

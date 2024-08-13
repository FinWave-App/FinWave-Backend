package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class TransactionConfig implements GroupedConfig {
    public int maxTransactionsInListPerRequest = 128;
    public int maxDescriptionLength = 256;

    public CategoryConfig categories = new CategoryConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }

    public static class CategoryConfig {
        public int maxCategoriesPerUser = 256;
        public int maxNameLength = 64;
        public int maxDescriptionLength = 128;
    }
}

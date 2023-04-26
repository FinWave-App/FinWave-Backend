package su.knst.fintrack.config.app;

import su.knst.fintrack.config.ConfigGroup;
import su.knst.fintrack.config.GroupedConfig;

public class AccountsConfig implements GroupedConfig {

    public int maxAccountsPerUser = 64;
    public AccountTagsConfig tags = new AccountTagsConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }

    public static class AccountTagsConfig {
        public int maxTagsPerUser = 32;
        public int maxNameLength = 64;
        public int maxDescriptionLength = 128;
    }
}

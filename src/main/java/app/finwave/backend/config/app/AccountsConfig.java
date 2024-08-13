package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class AccountsConfig implements GroupedConfig {

    public int maxAccountsPerUser = 64;
    public int maxNameLength = 64;
    public int maxDescriptionLength = 128;

    public AccountFoldersConfig folders = new AccountFoldersConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }

    public static class AccountFoldersConfig {
        public int maxFoldersPerUser = 32;
        public int maxNameLength = 64;
        public int maxDescriptionLength = 128;
    }
}

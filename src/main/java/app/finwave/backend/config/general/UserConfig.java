package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class UserConfig implements GroupedConfig {
    public int userSessionsLifetimeDays = 14;

    public int minLoginLength = 4;
    public int maxLoginLength = 64;

    public int minPasswordLength = 8;
    public int maxPasswordLength = 64;

    public int maxSessionDescriptionLength = 128;

    public RegistrationConfig registration = new RegistrationConfig();

    public boolean demoMode = false;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class RegistrationConfig {
        public boolean enabled = false;

        public String loginRegexFilter = "^[a-zA-Z0-9_-]+$";
        public String passwordRegexFilter = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*\\W)[A-Za-z\\d\\W]{8,}$";
    }
}

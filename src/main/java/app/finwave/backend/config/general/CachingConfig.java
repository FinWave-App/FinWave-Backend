package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class CachingConfig implements GroupedConfig {
    public Sessions sessions = new Sessions();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class Sessions {
        public int maxLists = 200;
        public int maxTokens = 500;
    }
}

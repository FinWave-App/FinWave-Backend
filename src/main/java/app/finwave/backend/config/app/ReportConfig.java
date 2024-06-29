package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class ReportConfig implements GroupedConfig {
    public int maxDescriptionLength = 128;
    public int expiresDays = 14;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

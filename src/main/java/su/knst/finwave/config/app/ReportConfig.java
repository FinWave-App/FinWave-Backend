package su.knst.finwave.config.app;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class ReportConfig implements GroupedConfig {
    public int maxDescriptionLength = 128;
    public int expiresDays = 14;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

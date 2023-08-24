package su.knst.finwave.config.app;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class AnalyticsConfig implements GroupedConfig {

    public int maxTimeRangeDaysForMonths = 365;
    public int maxTimeRangeDaysForDays = 120;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

package su.knst.fintrack.config.app;

import su.knst.fintrack.config.ConfigGroup;
import su.knst.fintrack.config.GroupedConfig;

public class AnalyticsConfig implements GroupedConfig {

    public int maxTimeRangeDaysForMonths = 365;
    public int maxTimeRangeDaysForDays = 120;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

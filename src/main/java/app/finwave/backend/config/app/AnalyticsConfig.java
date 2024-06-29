package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class AnalyticsConfig implements GroupedConfig {

    public int maxTimeRangeDaysForMonths = 366;
    public int maxTimeRangeDaysForDays = 120;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

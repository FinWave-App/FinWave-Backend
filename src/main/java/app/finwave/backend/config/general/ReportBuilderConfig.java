package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class ReportBuilderConfig implements GroupedConfig {
    public int maxTransactionsPerCycle = 100;
    public int threads = 4;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

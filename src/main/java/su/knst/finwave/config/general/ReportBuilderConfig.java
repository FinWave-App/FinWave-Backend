package su.knst.finwave.config.general;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class ReportBuilderConfig implements GroupedConfig {
    public int maxTransactionsPerCycle = 100;
    public int threads = 4;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

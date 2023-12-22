package su.knst.finwave.config.app;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class AccumulationConfig implements GroupedConfig {
    public int maxStepsPerAccount = 32;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

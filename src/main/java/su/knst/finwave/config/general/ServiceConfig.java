package su.knst.finwave.config.general;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class ServiceConfig implements GroupedConfig {

    public int threadPoolThreads = 2;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

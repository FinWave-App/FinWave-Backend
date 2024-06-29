package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class AccumulationConfig implements GroupedConfig {
    public int maxStepsPerAccount = 32;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

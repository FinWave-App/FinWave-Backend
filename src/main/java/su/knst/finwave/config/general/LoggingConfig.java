package su.knst.finwave.config.general;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class LoggingConfig implements GroupedConfig {
    public boolean trace = false;
    public boolean debug = false;
    public boolean info = false;
    public boolean warning = true;
    public boolean error = true;

    public boolean logFullClassName = false;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

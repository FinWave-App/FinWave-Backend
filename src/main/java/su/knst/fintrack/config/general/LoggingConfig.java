package su.knst.fintrack.config.general;

import su.knst.fintrack.config.ConfigGroup;
import su.knst.fintrack.config.GroupedConfig;

public class LoggingConfig implements GroupedConfig {
    public boolean trace = false;
    public boolean debug = false;
    public boolean info = true;
    public boolean warning = true;
    public boolean error = true;

    public boolean logFullClassName = false;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

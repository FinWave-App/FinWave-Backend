package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

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

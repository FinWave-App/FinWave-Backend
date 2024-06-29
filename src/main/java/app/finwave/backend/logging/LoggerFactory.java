package app.finwave.backend.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.LoggingConfig;

import static app.finwave.backend.Main.INJ;

public class LoggerFactory implements ILoggerFactory {
    protected static LoggingConfig config;

    static {
        config = INJ.getInstance(Configs.class).getState(new LoggingConfig());
    }

    @Override
    public Logger getLogger(String name) {
        if (!config.logFullClassName) {
            String[] split = name.split("\\.");
            name = split[split.length - 1];
        }

        return new app.finwave.backend.logging.Logger(name);
    }
}

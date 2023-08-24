package su.knst.finwave.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.LoggingConfig;

import static su.knst.finwave.Main.INJ;

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

        return new su.knst.finwave.logging.Logger(name);
    }
}

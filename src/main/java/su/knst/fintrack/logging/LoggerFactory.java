package su.knst.fintrack.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.general.LoggingConfig;

import static su.knst.fintrack.Main.INJ;

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

        return new su.knst.fintrack.logging.Logger(name);
    }
}

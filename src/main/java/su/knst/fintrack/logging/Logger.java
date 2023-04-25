package su.knst.fintrack.logging;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.general.LoggingConfig;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static su.knst.fintrack.Main.INJ;

public class Logger extends AbstractLogger {

    protected static LoggingConfig config;
    protected static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    static {
        config = INJ.getInstance(Configs.class).getState(new LoggingConfig());
    }

    protected String name;

    public Logger(String name) {
        this.name = name;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return name;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
        String formattedMessage = MessageFormatter.arrayFormat(messagePattern, arguments, throwable).getMessage();

        System.out.printf("[%s] [%s] [%s] %s%n",
                LocalTime.now().format(timeFormatter),
                name,
                level.toString(),
                formattedMessage
        );

        if (throwable != null)
            throwable.printStackTrace();
    }

    @Override
    public boolean isTraceEnabled() {
        return config.trace;
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return config.debug;
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return config.info;
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return config.warning;
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return config.error;
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }
}

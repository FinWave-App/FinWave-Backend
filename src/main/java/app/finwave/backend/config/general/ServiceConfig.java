package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class ServiceConfig implements GroupedConfig {
    public int threadPoolThreads = 2;
    public NotificationServiceConfig notifications = new NotificationServiceConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class NotificationServiceConfig {
        public int notificationsPerSecond = 100;
    }
}

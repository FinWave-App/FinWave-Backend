package su.knst.finwave.config.general;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

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

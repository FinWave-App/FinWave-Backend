package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class NotificationsConfig implements GroupedConfig {
    public int maxPointsPerUser = 64;
    public int maxDescriptionLength = 128;
    public int maxNotificationLength = 256;

    public WebPushConfig webPush = new WebPushConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }

    public static class WebPushConfig {
        public int maxEndpointLength = 2048;
        public int maxAuthLength = 128;
        public int maxP256dhLength = 128;
    }
}

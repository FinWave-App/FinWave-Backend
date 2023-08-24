package su.knst.finwave.config.general;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class HttpConfig implements GroupedConfig {
    public int port = 8080;
    public CorsConfig cors = new CorsConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class CorsConfig {
        public String allowedOrigins = "*";
        public String allowedMethods = "*";
        public String allowedHeaders = "*";
    }
}

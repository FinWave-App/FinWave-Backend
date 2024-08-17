package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

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

package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class HttpConfig implements GroupedConfig {
    public int port = 8080;
    public CorsConfig cors = new CorsConfig();
    public ProxyConfig outsideProxy = new ProxyConfig();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class CorsConfig {
        public String allowedOrigins = "*";
        public String allowedMethods = "*";
        public String allowedHeaders = "*";
    }

    public static class ProxyConfig {
        public boolean enabled = false;

        public String type = "socks";
        public String host = "";
        public int port = 0;

        public String username = "";
        public String password = "";
    }
}

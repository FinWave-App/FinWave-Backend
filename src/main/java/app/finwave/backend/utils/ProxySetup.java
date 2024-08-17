package app.finwave.backend.utils;

import app.finwave.backend.config.general.HttpConfig;
import app.finwave.backend.migration.Migrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ProxySetup {
    protected static final Logger log = LoggerFactory.getLogger(ProxySetup.class);

    public static void setup(HttpConfig.ProxyConfig config) {
        switch (config.type) {
            case "socks" -> {
                System.setProperty("socksProxyHost", config.host);
                System.setProperty("socksProxyPort", String.valueOf(config.port));
            }
            case "https" -> {
                System.setProperty("https.proxyHost", config.host);
                System.setProperty("https.proxyPort", String.valueOf(config.port));
            }
            case "http" -> {
                System.setProperty("http.proxyHost", config.host);
                System.setProperty("http.proxyPort", String.valueOf(config.port));
            }
        }

        if (config.username != null && config.password != null && !config.username.isBlank() && !config.password.isBlank()) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.username, config.password.toCharArray());
                }
            });
        }

        log.info("Successful proxy setup");
    }

}

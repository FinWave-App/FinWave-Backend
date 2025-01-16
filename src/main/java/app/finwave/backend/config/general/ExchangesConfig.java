package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class ExchangesConfig implements GroupedConfig {
    public Fawazahmed0Exchanges fawazahmed0Exchanges = new Fawazahmed0Exchanges();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class Fawazahmed0Exchanges {
        public boolean enabled = true;
        public int hoursCaching = 3;
        public String[] servers = new String[] {
                "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/",
                "https://latest.currency-api.pages.dev/v1/currencies/"
        };
    }
}

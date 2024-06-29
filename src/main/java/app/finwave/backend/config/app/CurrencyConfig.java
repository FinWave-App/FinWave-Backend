package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class CurrencyConfig implements GroupedConfig {
    public int maxCurrenciesPerUser = 128;
    public int maxCodeLength = 16;
    public int maxSymbolLength = 16;
    public int maxDecimals = 64;
    public int maxDescriptionLength = 128;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

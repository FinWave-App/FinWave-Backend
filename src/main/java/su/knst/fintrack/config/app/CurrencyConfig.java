package su.knst.fintrack.config.app;

import su.knst.fintrack.config.ConfigGroup;
import su.knst.fintrack.config.GroupedConfig;

public class CurrencyConfig implements GroupedConfig {
    public int maxCurrenciesPerUser = 128;
    public int maxCodeLength = 16;
    public int maxSymbolLength = 16;
    public int maxDescriptionLength = 128;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

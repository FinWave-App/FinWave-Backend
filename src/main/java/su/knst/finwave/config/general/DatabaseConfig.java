package su.knst.finwave.config.general;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

import java.util.Optional;

public class DatabaseConfig implements GroupedConfig {
    public String url = "jdbc:postgresql://postgres:5432/finwave";
    public String user = "finwave";
    public String password = Optional
            .ofNullable(System.getenv("DATABASE_PASSWORD"))
            .orElse("change_me");

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

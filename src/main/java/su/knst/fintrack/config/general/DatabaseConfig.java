package su.knst.fintrack.config.general;

import su.knst.fintrack.config.ConfigGroup;
import su.knst.fintrack.config.GroupedConfig;

import java.util.Optional;

public class DatabaseConfig implements GroupedConfig {
    public String url = "jdbc:postgresql://postgres:5432/fintrack";
    public String user = "fintrack";
    public String password = Optional
            .ofNullable(System.getenv("DATABASE_PASSWORD"))
            .orElse("change_me");

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

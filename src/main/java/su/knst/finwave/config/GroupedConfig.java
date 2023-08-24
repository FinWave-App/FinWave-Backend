package su.knst.finwave.config;

public interface GroupedConfig {
    ConfigGroup group();

    default String name() {
        String className = this.getClass().getSimpleName();

        return className.replace("Config", "").toLowerCase();
    }
}

package su.knst.fintrack.config;

public enum ConfigGroup {
    GENERAL("general"),
    APPLICATION("app");

    final String name;

    ConfigGroup(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

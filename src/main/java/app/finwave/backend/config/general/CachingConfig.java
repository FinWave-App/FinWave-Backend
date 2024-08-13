package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class CachingConfig implements GroupedConfig {
    public Sessions sessions = new Sessions();
    public CategoriesBudget categoriesBudget = new CategoriesBudget();
    public Analytics analytics = new Analytics();
    public Ai ai = new Ai();
    public Files files = new Files();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class Sessions {
        public int maxLists = 200;
        public int maxTokens = 500;
    }

    public static class CategoriesBudget {
        public int maxLists = 200;
    }

    public static class Analytics {
        public int maxDaysEntries = 200;
        public int maxMonthsEntries = 200;
        public int maxCategoriesSummingEntries = 200;
    }

    public static class Files {
        public int maxFiles = 500;
        public int maxLists = 200;
        public int maxUsages = 1000;
    }

    public static class Ai {
        public int maxContexts = 200;
    }
}

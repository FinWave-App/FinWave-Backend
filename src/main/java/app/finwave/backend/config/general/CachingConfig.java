package app.finwave.backend.config.general;

import app.finwave.backend.api.analytics.AnalyticsManager;
import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class CachingConfig implements GroupedConfig {
    public Sessions sessions = new Sessions();
    public TagsManagement tagsManagement = new TagsManagement();
    public Analytics analytics = new Analytics();

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }

    public static class Sessions {
        public int maxLists = 200;
        public int maxTokens = 500;
    }

    public static class TagsManagement {
        public int maxLists = 200;
    }

    public static class Analytics {
        public int maxDaysEntries = 200;
        public int maxMonthsEntries = 200;
        public int maxTagSummingEntries = 200;
    }
}

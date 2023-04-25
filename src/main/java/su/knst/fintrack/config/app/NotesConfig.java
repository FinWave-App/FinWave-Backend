package su.knst.fintrack.config.app;

import su.knst.fintrack.config.ConfigGroup;
import su.knst.fintrack.config.GroupedConfig;

public class NotesConfig implements GroupedConfig {
    public int maxNoteLength = 1024;
    public int maxNotesInListPerRequest = 64;
    public int minFilterLength = 5;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

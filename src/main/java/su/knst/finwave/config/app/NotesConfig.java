package su.knst.finwave.config.app;

import su.knst.finwave.config.ConfigGroup;
import su.knst.finwave.config.GroupedConfig;

public class NotesConfig implements GroupedConfig {
    public int maxNoteLength = 1024;
    public int maxNotesPerUser = 512;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

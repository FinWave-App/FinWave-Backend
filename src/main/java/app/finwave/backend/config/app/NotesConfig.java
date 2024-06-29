package app.finwave.backend.config.app;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class NotesConfig implements GroupedConfig {
    public int maxNoteLength = 1024;
    public int maxNotesPerUser = 512;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.APPLICATION;
    }
}

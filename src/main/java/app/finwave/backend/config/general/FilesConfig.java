package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class FilesConfig implements GroupedConfig {
    public long maxMiBStoragePerUser = 512;
    public int maxUploadedFilesExpiredDays = 14;

    public int maxUploadedFilesDescription = 128;
    public int maxUploadedFilesName = 128;

    public int filesToDeletePerHour = 50;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}

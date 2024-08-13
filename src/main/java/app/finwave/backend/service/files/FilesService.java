package app.finwave.backend.service.files;

import app.finwave.backend.api.files.FilesDatabase;
import app.finwave.backend.api.files.FilesManager;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.FilesConfig;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import app.finwave.backend.api.report.ReportDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.service.AbstractService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class FilesService extends AbstractService {
    protected FilesConfig config;
    protected FilesManager filesManager;

    @Inject
    public FilesService(FilesManager filesManager, Configs configs) {
        this.filesManager = filesManager;
        this.config = configs.getState(new FilesConfig());
    }

    @Override
    public void run() {
        filesManager.deleteExpired(config.filesToDeletePerHour);
    }

    @Override
    public long getRepeatTime() {
        return 1;
    }

    @Override
    public long getInitDelay() {
        return 0;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.HOURS;
    }

    @Override
    public String name() {
        return "Files Garbage";
    }
}

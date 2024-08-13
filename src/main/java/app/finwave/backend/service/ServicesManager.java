package app.finwave.backend.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.ServiceConfig;
import app.finwave.backend.config.general.UserConfig;
import app.finwave.backend.service.demo.DemoService;
import app.finwave.backend.service.notes.NotesService;
import app.finwave.backend.service.notifications.NotificationsService;
import app.finwave.backend.service.recurring.RecurringService;
import app.finwave.backend.service.files.FilesService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class ServicesManager {
    protected ScheduledExecutorService scheduledExecutorService;
    protected ServiceConfig config;

    protected static final Logger log = LoggerFactory.getLogger(ServicesManager.class);

    @Inject
    public ServicesManager(Configs configs,
                           RecurringService recurringService,
                           NotificationsService notificationsService,
                           NotesService notesService,
                           FilesService filesService,
                           DemoService demoService) {
        this.config = configs.getState(new ServiceConfig());

        var userConfig = configs.getState(new UserConfig());

        this.scheduledExecutorService = Executors.newScheduledThreadPool(config.threadPoolThreads);

        initService(recurringService);
        initService(notificationsService);
        initService(notesService);
        initService(filesService);

        if (userConfig.demoMode) {
            initService(demoService);

            log.warn("THE SERVER IS IN DEMO MODE. ALL DATA WILL BE DELETED EVERY 24 HOURS");
        }
    }

    public void initService(AbstractService service) {
        scheduledExecutorService.scheduleAtFixedRate(() -> runService(service),
                service.getInitDelay(),
                service.getRepeatTime(),
                service.getTimeUnit()
        );
    }

    protected void runService(AbstractService service) {
        try {
            service.run();
        }catch (Throwable e) {
            log.error("Failed to run service '" + service.name() + "'", e);
        }
    }

}

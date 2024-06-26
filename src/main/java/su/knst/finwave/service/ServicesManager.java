package su.knst.finwave.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.ServiceConfig;
import su.knst.finwave.config.general.UserConfig;
import su.knst.finwave.service.demo.DemoService;
import su.knst.finwave.service.notes.NotesService;
import su.knst.finwave.service.notifications.NotificationsService;
import su.knst.finwave.service.recurring.RecurringService;
import su.knst.finwave.service.reports.ReportsService;

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
                           ReportsService reportsService,
                           DemoService demoService) {
        this.config = configs.getState(new ServiceConfig());

        var userConfig = configs.getState(new UserConfig());

        this.scheduledExecutorService = Executors.newScheduledThreadPool(config.threadPoolThreads);

        initService(recurringService);
        initService(notificationsService);
        initService(notesService);
        initService(reportsService);

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

package su.knst.finwave.service.notifications;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import su.knst.finwave.api.notification.NotificationDatabase;
import su.knst.finwave.api.notification.data.Notification;
import su.knst.finwave.api.notification.manager.NotificationManager;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.ServiceConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.service.AbstractService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class NotificationsService extends AbstractService {
    protected NotificationManager manager;
    protected NotificationDatabase database;
    protected ServiceConfig.NotificationServiceConfig config;

    @Inject
    public NotificationsService(NotificationManager manager, DatabaseWorker databaseWorker, Configs configs) {
        this.manager = manager;
        this.database = databaseWorker.get(NotificationDatabase.class);
        this.config = configs.getState(new ServiceConfig()).notifications;
    }

    @Override
    public void run() {
        List<Notification> notifications = database.pullNotifications(config.notificationsPerSecond);

        for (Notification notification : notifications) {
            manager.pushImmediately(notification);
        }
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
        return TimeUnit.SECONDS;
    }

    @Override
    public String name() {
        return "Notifications";
    }
}

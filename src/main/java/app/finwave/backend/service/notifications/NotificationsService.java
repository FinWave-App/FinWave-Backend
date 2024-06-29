package app.finwave.backend.service.notifications;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import app.finwave.backend.api.notification.NotificationDatabase;
import app.finwave.backend.api.notification.data.Notification;
import app.finwave.backend.api.notification.manager.NotificationManager;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.ServiceConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.service.AbstractService;

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

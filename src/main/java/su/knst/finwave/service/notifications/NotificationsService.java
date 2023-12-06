package su.knst.finwave.service.notifications;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import su.knst.finwave.api.notification.NotificationDatabase;
import su.knst.finwave.api.notification.data.Notification;
import su.knst.finwave.api.transaction.manager.TransactionsManager;
import su.knst.finwave.api.transaction.recurring.RecurringTransactionDatabase;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.NotificationsPointsRecord;
import su.knst.finwave.service.AbstractService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class NotificationsService extends AbstractService {
    protected NotificationDatabase database;
    protected NotificationPusher pusher;

    @Inject
    public NotificationsService(DatabaseWorker databaseWorker, NotificationPusher pusher) {
        this.database = databaseWorker.get(NotificationDatabase.class);
        this.pusher = pusher;
    }

    @Override
    public void run() {
        List<Notification> notifications = database.pullNotifications(100);

        for (Notification notification : notifications) {
            long targetPointId = notification.options().pointId();
            List<NotificationsPointsRecord> records = database.getUserNotificationsPoints(notification.userId());

            if (targetPointId != -1)
                records = records.stream().filter((r) -> r.getId() == targetPointId).toList();

            if (records.size() == 0)
                continue;

            boolean pushed = false;

            for (NotificationsPointsRecord record : records) {
                if (!record.getIsPrimary() && pushed)
                    break;

                pushed = pusher.push(record, notification);
            }
        }
    }

    @Override
    public long getRepeatTime() {
        return 5;
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

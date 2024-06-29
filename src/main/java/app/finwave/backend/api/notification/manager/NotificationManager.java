package app.finwave.backend.api.notification.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import app.finwave.backend.api.notification.NotificationDatabase;
import app.finwave.backend.api.notification.NotificationPusher;
import app.finwave.backend.api.notification.data.Notification;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.ServiceConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.NotificationsPointsRecord;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class NotificationManager {

    protected NotificationDatabase database;
    protected NotificationPusher pusher;

    protected ExecutorService threadPool = Executors.newFixedThreadPool(2);

    protected float rate;
    protected long lastPushTime;
    protected ReentrantLock lock = new ReentrantLock();

    protected ServiceConfig.NotificationServiceConfig config;

    @Inject
    public NotificationManager(DatabaseWorker databaseWorker, NotificationPusher pusher, Configs configs) {
        this.database = databaseWorker.get(NotificationDatabase.class);
        this.pusher = pusher;
        this.config = configs.getState(new ServiceConfig()).notifications;
    }

    public CompletableFuture<PushResult> push(Notification notification) {
        CompletableFuture<PushResult> result = new CompletableFuture<>();

        boolean pushImmediately = updateRate();

        if (!pushImmediately) {
            database.saveNotification(notification);
            result.complete(PushResult.SAVED_TO_PULL);
            return result;
        }

        threadPool.submit(() -> {
            result.complete(pushImmediately(notification) ? PushResult.PUSHED : PushResult.FAILED);
        });

        return result;
    }

    public boolean pushImmediately(Notification notification) {
        boolean pushed = false;

        long targetPointId = notification.options().pointId();
        List<NotificationsPointsRecord> records = database.getUserNotificationsPoints(notification.userId());

        if (targetPointId != -1)
            records = records.stream().filter((r) -> r.getId() == targetPointId).toList();

        if (records.size() == 0)
            return false;

        for (NotificationsPointsRecord record : records) {
            if (!record.getIsPrimary() && pushed)
                break;

            try {
                pushed = pusher.push(record, notification).get();
            } catch (Exception e) {
                pushed = false;
            }
        }

        return pushed;
    }

    protected boolean updateRate(int count) {
        lock.lock();

        try {
            long time = System.nanoTime();
            float deltaPushTime = (time - lastPushTime) / 1_000_000_000f;
            lastPushTime = time;

            rate = Math.max(rate + count - (config.notificationsPerSecond * deltaPushTime) , 0);

            return rate < config.notificationsPerSecond;
        }finally {
            lock.unlock();
        }
    }

    protected boolean updateRate() {
        return updateRate(1);
    }
}

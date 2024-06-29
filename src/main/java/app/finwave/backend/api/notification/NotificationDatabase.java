package app.finwave.backend.api.notification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import app.finwave.backend.api.notification.data.Notification;
import app.finwave.backend.api.notification.data.NotificationOptions;
import app.finwave.backend.api.notification.data.point.AbstractNotificationPointData;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.NotificationsPointsRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.api.ApiResponse.GSON;
import static app.finwave.backend.jooq.Tables.*;

public class NotificationDatabase extends AbstractDatabase {
    public NotificationDatabase(DSLContext context) {
        super(context);
    }

    public void saveNotification(Notification notification) {
        context.insertInto(NOTIFICATIONS_PULL)
                .set(NOTIFICATIONS_PULL.TEXT, notification.text())
                .set(NOTIFICATIONS_PULL.OPTIONS, JSONB.valueOf(GSON.toJson(notification.options())))
                .set(NOTIFICATIONS_PULL.USER_ID, notification.userId())
                .set(NOTIFICATIONS_PULL.CREATED_AT, notification.createdAt())
                .execute();
    }

    public List<Notification> pullNotifications(int count) {
        return context.deleteFrom(NOTIFICATIONS_PULL)
                .orderBy(NOTIFICATIONS_PULL.CREATED_AT.asc())
                .limit(count)
                .returningResult(NOTIFICATIONS_PULL.fields())
                .fetch().map((r) -> new Notification(
                        r.get(NOTIFICATIONS_PULL.ID),
                        r.get(NOTIFICATIONS_PULL.TEXT),
                        GSON.fromJson(r.get(NOTIFICATIONS_PULL.OPTIONS).data(), NotificationOptions.class),
                        r.get( NOTIFICATIONS_PULL.USER_ID),
                        r.get(NOTIFICATIONS_PULL.CREATED_AT)
                ));
    }

    public List<NotificationsPointsRecord> getUserNotificationsPoints(int userId) {
        return context.selectFrom(NOTIFICATIONS_POINTS)
                .where(NOTIFICATIONS_POINTS.USER_ID.eq(userId))
                .orderBy(NOTIFICATIONS_POINTS.IS_PRIMARY.desc(), NOTIFICATIONS_POINTS.CREATED_AT.desc())
                .fetch();
    }

    public Optional<Long> registerNotificationPoint(int userId, boolean isPrimary, AbstractNotificationPointData data, String description) {
        return context.insertInto(NOTIFICATIONS_POINTS)
                .set(NOTIFICATIONS_POINTS.IS_PRIMARY, isPrimary)
                .set(NOTIFICATIONS_POINTS.USER_ID, userId)
                .set(NOTIFICATIONS_POINTS.TYPE, (short) data.type.ordinal())
                .set(NOTIFICATIONS_POINTS.CREATED_AT, OffsetDateTime.now())
                .set(NOTIFICATIONS_POINTS.DATA, JSONB.valueOf(GSON.toJson(data)))
                .set(NOTIFICATIONS_POINTS.DESCRIPTION, description)
                .returningResult(NOTIFICATIONS_POINTS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public int getPointsCount(int userId) {
        return context.selectCount()
                .from(NOTIFICATIONS_POINTS)
                .where(NOTIFICATIONS_POINTS.USER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public void editNotificationPointPrimary(long pointId, boolean isPrimary) {
        context.update(NOTIFICATIONS_POINTS)
                .set(NOTIFICATIONS_POINTS.IS_PRIMARY, isPrimary)
                .where(NOTIFICATIONS_POINTS.ID.eq(pointId))
                .execute();
    }

    public void editNotificationPointDescription(long pointId, String description) {
        context.update(NOTIFICATIONS_POINTS)
                .set(NOTIFICATIONS_POINTS.DESCRIPTION, description)
                .where(NOTIFICATIONS_POINTS.ID.eq(pointId))
                .execute();
    }

    public void deleteNotificationPoint(long pointId) {
        context.deleteFrom(NOTIFICATIONS_POINTS)
                .where(NOTIFICATIONS_POINTS.ID.eq(pointId))
                .execute();
    }

    public boolean userOwnPoint(int userId, long pointId) {
        return context.select(NOTIFICATIONS_POINTS.ID)
                .from(NOTIFICATIONS_POINTS)
                .where(NOTIFICATIONS_POINTS.USER_ID.eq(userId).and(NOTIFICATIONS_POINTS.ID.eq(pointId)))
                .fetchOptional()
                .isPresent();
    }
}

package su.knst.finwave.api.notification.data;

import java.time.OffsetDateTime;

public record Notification(long id, String text, NotificationOptions options, int userId, OffsetDateTime createdAt) {
    public static Notification create(String text, NotificationOptions options, int userId) {
        return new Notification(-1, text, options, userId, OffsetDateTime.now());
    }
}

package app.finwave.backend.api.notification.data;

public record NotificationOptions(boolean silent, long pointId, Object args) {
}

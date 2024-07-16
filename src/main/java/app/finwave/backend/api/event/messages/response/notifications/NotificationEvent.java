package app.finwave.backend.api.event.messages.response.notifications;

import app.finwave.backend.api.event.messages.ResponseMessage;
import app.finwave.backend.api.notification.data.Notification;

public class NotificationEvent extends ResponseMessage<Notification> {
    public NotificationEvent(Notification notification) {
        super("notification", notification);
    }
}

package app.finwave.backend.api.event.messages.response.notifications;

import app.finwave.backend.api.event.messages.MessageBody;
import app.finwave.backend.api.event.messages.ResponseMessage;

import java.util.UUID;

public class NotificationPointRegistered extends ResponseMessage<NotificationPointRegistered.NotificationPointBody> {
    public NotificationPointRegistered(long id, UUID uuid) {
        super("newNotificationRegistered", new NotificationPointBody(id, uuid));
    }

    protected static class NotificationPointBody extends MessageBody {
        public final long id;
        public final UUID uuid;

        public NotificationPointBody(long id, UUID uuid) {
            this.id = id;
            this.uuid = uuid;
        }
    }
}

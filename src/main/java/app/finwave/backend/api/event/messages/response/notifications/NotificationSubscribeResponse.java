package app.finwave.backend.api.event.messages.response.notifications;

import app.finwave.backend.api.event.messages.MessageBody;
import app.finwave.backend.api.event.messages.ResponseMessage;

public class NotificationSubscribeResponse extends ResponseMessage<NotificationSubscribeResponse.SubscribeBody> {
    public NotificationSubscribeResponse(String status) {
        super("subscribeNotification", new SubscribeBody(status));
    }

    protected static class SubscribeBody extends MessageBody {
        public final String status;

        public SubscribeBody(String status) {
            this.status = status;
        }
    }
}

package app.finwave.backend.api.event.messages.response;

import app.finwave.backend.api.event.messages.MessageBody;
import app.finwave.backend.api.event.messages.ResponseMessage;

public class NotifyUpdate extends ResponseMessage<NotifyUpdate.NotifyBody> {
    public NotifyUpdate(String updated) {
        super("update", new NotifyBody(updated));
    }

    protected static class NotifyBody extends MessageBody {
        public final String updated;

        public NotifyBody(String updated) {
            this.updated = updated;
        }
    }
}



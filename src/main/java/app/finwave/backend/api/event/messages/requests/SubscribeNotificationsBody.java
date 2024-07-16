package app.finwave.backend.api.event.messages.requests;

import app.finwave.backend.api.event.messages.MessageBody;

import java.util.UUID;

public class SubscribeNotificationsBody extends MessageBody {
    public final UUID pointUUID;

    public SubscribeNotificationsBody(UUID pointUUID) {
        this.pointUUID = pointUUID;
    }
}

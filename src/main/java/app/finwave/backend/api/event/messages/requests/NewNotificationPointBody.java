package app.finwave.backend.api.event.messages.requests;

import app.finwave.backend.api.event.messages.MessageBody;

public class NewNotificationPointBody extends MessageBody {
    public final String description;
    public final boolean isPrimary;

    public NewNotificationPointBody(String description, boolean isPrimary) {
        this.description = description;
        this.isPrimary = isPrimary;
    }
}

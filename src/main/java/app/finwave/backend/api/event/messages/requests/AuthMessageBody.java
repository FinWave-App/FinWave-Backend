package app.finwave.backend.api.event.messages.requests;

import app.finwave.backend.api.event.messages.MessageBody;

public class AuthMessageBody extends MessageBody {
    public final String token;

    public AuthMessageBody(String token) {
        this.token = token;
    }
}

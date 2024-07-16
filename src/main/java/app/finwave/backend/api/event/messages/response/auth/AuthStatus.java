package app.finwave.backend.api.event.messages.response.auth;

import app.finwave.backend.api.event.messages.MessageBody;
import app.finwave.backend.api.event.messages.ResponseMessage;

public class AuthStatus extends ResponseMessage<AuthStatus.AuthBody> {
    public AuthStatus(String status) {
        super("auth", new AuthBody(status));
    }

    protected static class AuthBody extends MessageBody {
        public final String status;

        public AuthBody(String status) {
            this.status = status;
        }
    }
}

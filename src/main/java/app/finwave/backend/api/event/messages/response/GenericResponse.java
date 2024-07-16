package app.finwave.backend.api.event.messages.response;

import app.finwave.backend.api.event.messages.MessageBody;
import app.finwave.backend.api.event.messages.ResponseMessage;

public class GenericResponse extends ResponseMessage<GenericResponse.GenericMessage> {
    public GenericResponse(String message, int code) {
        super("generic", new GenericMessage(message, code));
    }

    public GenericResponse(String message) {
        super("generic", new GenericMessage(message, 0));
    }

    protected static class GenericMessage extends MessageBody {
        public final String message;
        public final int code;

        public GenericMessage(String message, int code) {
            this.message = message;
            this.code = code;
        }
    }
}

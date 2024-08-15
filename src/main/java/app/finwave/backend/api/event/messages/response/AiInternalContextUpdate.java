package app.finwave.backend.api.event.messages.response;

import app.finwave.backend.api.event.messages.MessageBody;
import app.finwave.backend.api.event.messages.ResponseMessage;

public class AiInternalContextUpdate extends ResponseMessage<AiInternalContextUpdate.ContextUpdate> {
    public AiInternalContextUpdate(long contextId, String newMessage, String role) {
        super("aiUpdate", new ContextUpdate(contextId, newMessage, role));
    }

    protected static class ContextUpdate extends MessageBody {
        public final long contextId;
        public final String newMessage;
        public final String role;

        protected ContextUpdate(long contextId, String newMessage, String role) {
            this.contextId = contextId;
            this.newMessage = newMessage;
            this.role = role;
        }
    }
}

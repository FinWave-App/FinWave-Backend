package app.finwave.backend.api.event.messages;

public class ResponseMessage<T extends MessageBody> {
    public final String type;
    protected final T body;

    public ResponseMessage(String type, T body) {
        this.type = type;
        this.body = body;
    }
}

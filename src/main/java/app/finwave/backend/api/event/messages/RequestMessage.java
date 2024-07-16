package app.finwave.backend.api.event.messages;

import app.finwave.backend.api.ApiResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RequestMessage {
    public final String type;
    protected final JsonElement body;

    public RequestMessage(String type, JsonObject body) {
        this.type = type;
        this.body = body;
    }

    public <T extends MessageBody> T getBody(Class<T> clazz) {
        return ApiResponse.GSON.fromJson(body, clazz);
    }
}

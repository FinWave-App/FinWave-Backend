package su.knst.fintrack.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.OffsetDateTime;

public abstract class ApiResponse {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, (JsonDeserializer<OffsetDateTime>)
                    (j, t, c) -> OffsetDateTime.parse(j.getAsString()))
            .registerTypeAdapter(OffsetDateTime.class, (JsonSerializer<OffsetDateTime>)
                    (o, t, c) -> c.serialize(o.toString()))
            .create();

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}

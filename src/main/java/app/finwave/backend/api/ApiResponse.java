package app.finwave.backend.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

public abstract class ApiResponse {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, (JsonDeserializer<OffsetDateTime>)
                    (j, t, c) -> OffsetDateTime.parse(j.getAsString()))
            .registerTypeAdapter(OffsetDateTime.class, (JsonSerializer<OffsetDateTime>)
                    (o, t, c) -> c.serialize(o.toString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>)
                    (j, t, c) -> LocalDate.parse(j.getAsString()))
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>)
                    (o, t, c) -> c.serialize(o.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (j, t, c) -> LocalDateTime.parse(j.getAsString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (o, t, c) -> c.serialize(o.toString()))
            .registerTypeAdapter(Optional.class, (JsonSerializer<Optional>)
                    (o, t, c) -> c.serialize(o.orElse(null)))
            .create();

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}

package app.finwave.backend.api.ai.content;

import com.google.gson.JsonElement;

import java.util.List;

public record ContentMeta(List<JsonElement> toolCalls, String toolCallId) {
}

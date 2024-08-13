package app.finwave.backend.api.ai.tools;

import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.ai.content.ContentMeta;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.stefanbratanov.jvm.openai.ContentPart;
import org.jooq.JSON;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class ContentPartParser {
    public static List<Object> jsonToContent(JSON json) {
        JsonReader reader = ApiResponse.GSON.newJsonReader(new StringReader(json.data()));
        ArrayList<Object> contentParts = new ArrayList<>();

        try {
            reader.beginArray();

            while (reader.hasNext()) {
                reader.beginObject();
                HashMap<String, JsonElement> map = new HashMap<>();

                while (reader.hasNext())
                    map.put(reader.nextName(), ApiResponse.GSON.fromJson(reader, JsonElement.class));

                reader.endObject();

                String type = map.get("type").getAsString();

                switch (type) {
                    case "text" -> contentParts.add(
                            ContentPart.textContentPart(map.get("text").getAsString())
                    );
                    case "image_url" -> {
                        var imageUrl = ApiResponse.GSON.fromJson(map.get("imageUrl"), ContentPart.ImageUrlContentPart.ImageUrl.class);

                        var part = imageUrl.detail() != null && imageUrl.detail().isPresent() ?
                                ContentPart.imageUrlContentPart(imageUrl.url(), imageUrl.detail().get()) :
                                ContentPart.imageUrlContentPart(imageUrl.url());

                        contentParts.add(part);
                    }
                    case "image_file" -> {
                        var imageFile = ApiResponse.GSON.fromJson(map.get("imageFile"), ContentPart.ImageFileContentPart.ImageFile.class);

                        var part = imageFile.detail() != null && imageFile.detail().isPresent() ?
                                ContentPart.imageFileContentPart(imageFile.fileId(), imageFile.detail().get()) :
                                ContentPart.imageFileContentPart(imageFile.fileId());

                        contentParts.add(part);
                    }
                    case "meta" -> {
                        var meta = ApiResponse.GSON.fromJson(map.get("meta"), ContentMeta.class);

                        contentParts.add(meta);
                    }
                }
            }

            reader.endArray();
        }catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.unmodifiableList(contentParts);
    }

    public static JSON contentToJson(List<Object> content) {
        StringWriter stringWriter = new StringWriter();

        try {
            JsonWriter writer = ApiResponse.GSON.newJsonWriter(stringWriter);

            writer.beginArray();

            for (Object object : content) {
                writer.beginObject();
                if (object instanceof ContentPart contentPart) {
                    String type = contentPart.type();

                    writer.name("type").value(type);
                    Map<String, JsonElement> contentMap = ApiResponse.GSON.toJsonTree(contentPart).getAsJsonObject().asMap();

                    for (Map.Entry<String, JsonElement> entry : contentMap.entrySet())
                        writer.name(entry.getKey()).jsonValue(entry.getValue().toString());
                }else {
                    writer.name("type").value("meta");
                    writer.name("meta").jsonValue(ApiResponse.GSON.toJson(object));
                }

                writer.endObject();
            }

            writer.endArray();

            writer.flush();
            writer.close();
        }catch (IOException e) {
            e.printStackTrace();

            return JSON.json("[]");
        }

        return JSON.json(stringWriter.toString());
    }

}

package app.finwave.backend.api.ai;

import app.finwave.backend.api.ai.content.ContentMeta;
import app.finwave.backend.api.ai.tools.AiTools;
import app.finwave.backend.api.ai.tools.ArrayDeserializer;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.ai.tools.ChatMessagesBuilder;
import app.finwave.backend.api.files.FilesManager;
import app.finwave.backend.api.files.LimitedWithCallbackOutputStream;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.AiConfig;
import app.finwave.backend.jooq.tables.records.AiMessagesRecord;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.scw.utils.gson.G;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.stefanbratanov.jvm.openai.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jooq.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static app.finwave.backend.api.ai.tools.ContentPartParser.contentToJson;
import static app.finwave.backend.api.ai.tools.ContentPartParser.jsonToContent;

@Singleton
public class AiWorker {
    protected static final Logger log = LoggerFactory.getLogger(AiWorker.class);

    protected List<Tool> toolsList;
    protected AiTools aiTools;

    protected AiConfig config;
    protected OpenAI ai;
    protected AiManager manager;

    protected FilesManager filesManager;

    protected String filesApiRoute = System.getenv("API_URL") + "files/download";

    @Inject
    public AiWorker(Configs configs, AiManager manager, FilesManager filesManager, AiTools aiTools) {
        this.config = configs.getState(new AiConfig());
        this.filesManager = filesManager;

        if (!config.enabled)
            return;

        this.aiTools = aiTools;

        OpenAI.Builder builder = OpenAI.newBuilder(config.token);

        if (!config.customUrl.isBlank())
            builder.baseUrl(config.customUrl);

        if (!config.project.isBlank())
            builder.project(config.project);

        if (!config.organization.isBlank())
            builder.organization(config.organization);

        this.ai = builder.build();
        this.toolsList = aiTools.getTools();
        this.manager = manager;
    }

    public Optional<Long> initContext(UsersSessionsRecord session, String additionalSystemMessage) {
        log.debug("Creating new context for user #{}", session.getUserId());

        Optional<Long> result = manager.newContext(session.getUserId());

        if (result.isEmpty())
            return Optional.empty();

        String systemMessage = config.baseSystemMessage
                .replace(
                        "{_ADDITIONAL_}",
                        additionalSystemMessage != null && !additionalSystemMessage.isBlank() ? additionalSystemMessage : "")
                .replace(
                        "{_DATETIME_}",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        manager.pushMessage(result.get(), "system", contentToJson(List.of(
                new ContentPart.TextContentPart(systemMessage)
        )), null);

        return result;
    }

    protected List<ToolCall> checkUndoneRequests(List<AiMessagesRecord> messages) {
        AiMessagesRecord lastMessage = messages.get(messages.size() - 1);

        if (!lastMessage.getRole().equals("assistant"))
            return List.of();

        ContentMeta meta = jsonToContent(lastMessage.getContent())
                .stream()
                .filter((o) -> o instanceof ContentMeta)
                .map((o) -> (ContentMeta) o)
                .findFirst()
                .orElse(null);

        if (meta == null)
            return List.of();

        return meta.toolCalls()
                    .stream()
                    .filter((e) -> e.getAsJsonObject().asMap().containsKey("function"))
                    .map((e) -> ApiResponse.GSON.fromJson(e, ToolCall.FunctionToolCall.class))
                    .map((e) -> (ToolCall) e)
                    .toList();
    }

    protected boolean runTools(List<ToolCall> toolCalls, long contextId, UsersSessionsRecord session) {
        boolean result = false;

        Gson g = ApiResponse.GSON
                .newBuilder()
                .registerTypeAdapter(new TypeToken<Map<String, String>>(){}.getType(), new ArrayDeserializer())
                .create();

        for (ToolCall toolCall : toolCalls) {
            if (toolCall instanceof ToolCall.FunctionToolCall functionCall) {
                String name = functionCall.function().name();

                Map<String, String> args = g.fromJson(functionCall.function().arguments(), new TypeToken<Map<String, String>>(){}.getType());

                Object toolResult = aiTools.run(name, session, args);

                manager.pushMessage(contextId, "tool", contentToJson(List.of(
                        new ContentPart.TextContentPart(ApiResponse.GSON.toJson(toolResult)),
                        new ContentMeta(null, toolCall.id())
                )), null);

                result = true;
            }
        }

        return result;
    }

    protected String ask(long contextId, UsersSessionsRecord session) {
        List<AiMessagesRecord> messages = manager.getMessages(contextId);
        List<ToolCall> undoneRequests = checkUndoneRequests(messages);

        if (!undoneRequests.isEmpty())
            runTools(undoneRequests, contextId, session);

        ChatCompletion chatCompletion = ai.chatClient().createChatCompletion(createChatCompletionRequest(config.includeTools, messages));
        Usage usage = chatCompletion.usage();
        ChatCompletion.Choice.Message message = chatCompletion.choices().get(0).message();

        ArrayList<Object> contents = new ArrayList<>();

        if (message.content() != null)
            contents.add(ContentPart.textContentPart(message.content()));

        if (message.toolCalls() != null && !message.toolCalls().isEmpty())
            contents.add(new ContentMeta(message.toolCalls().stream().map(G.GSON::toJsonTree).toList(), null));

        boolean result = manager.pushMessage(contextId, "assistant", contentToJson(contents), usage);

        if (!result)
            return null;

        if (message.toolCalls() != null && runTools(message.toolCalls(), contextId, session))
            return ask(contextId, session);

        return message.content();
    }

    public String ask(long contextId, UsersSessionsRecord session, List<ContentPart> contentParts) {
        log.debug("AI asking in #{} context for user #{}", contextId, session.getUserId());

        if (contentParts != null && !contentParts.isEmpty())
            pushMessage(contextId, "user", contentParts);

        return ask(contextId, session);
    }

    public boolean pushMessage(long contextId, String role, List<ContentPart> contentParts) {
        return manager.pushMessage(contextId, role,
                contentToJson(contentParts.stream().map(c -> (Object) c).toList()),
                null
        );
    }

    public boolean attachFiles(long contextId, List<FilesRecord> files) {
        boolean result = false;

        for (FilesRecord record : files) {
            String mime = record.getMimeType();

            boolean notExactly = false;

            result = switch (mime) {
                case "image/jpeg", "image/png", "image/gif", "image/webp" -> attachImages(List.of(record), contextId);
                case "application/pdf" -> attachPDFs(List.of(record), contextId);
                default -> {
                    notExactly = true;

                    yield false;
                }
            };

            if (notExactly) {
                String geneticType = mime.split("/")[0];

                if (geneticType.equals("text"))
                    result = attachTexts(List.of(record), contextId);
            }

            if (!result)
                return false;
        }

        return result;
    }

    protected boolean sizeValid(List<FilesRecord> files) {
        long sizeSum = 0;

        for (FilesRecord file : files) {
            sizeSum += file.getSize();

            if (sizeSum > config.maxFilesSizeSumPerAttachmentKiB * 1024L)
                return false;
        }

        return true;
    }

    protected boolean attachPDFs(List<FilesRecord> files, long contextId) {
        ArrayList<FilesRecord> images = new ArrayList<>();
        ArrayList<FilesRecord> texts = new ArrayList<>();

        for (FilesRecord record : files) {
            Optional<File> optionalFile = filesManager.getFile(record);

            if (optionalFile.isEmpty())
                return false;

            try (PDDocument document = PDDocument.load(optionalFile.get())) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                PDFRenderer renderer = new PDFRenderer(document);

                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);

                    Optional<LimitedWithCallbackOutputStream> streamOptional = filesManager.registerAndOpenStream(record.getOwnerId(),
                            record.getCreatedAt(), record.getExpiresAt(), record.getIsPublic(),
                            "aiFileConverter", "image/png", record.getName() + " #" + (page + 1), record.getDescription()
                    );

                    if (streamOptional.isEmpty())
                        return false;

                    LimitedWithCallbackOutputStream stream = streamOptional.get();
                    ImageIO.write(image, "PNG", stream);

                    Optional<String> fileId = filesManager.getRecordFromStream(stream).map(FilesRecord::getId); // get only fileId, because after close stream record change
                    stream.close();

                    if (fileId.isEmpty())
                        return false;

                    images.add(filesManager.getFileRecord(fileId.get()).orElseThrow());
                }

                String text = pdfStripper.getText(document);

                if (text != null && !text.isBlank()) {
                    Optional<LimitedWithCallbackOutputStream> streamOptional = filesManager.registerAndOpenStream(record.getOwnerId(),
                            record.getCreatedAt(), record.getExpiresAt(), record.getIsPublic(),
                            "aiFileConverter", "text/plain", record.getName() + " [plain text]", record.getDescription()
                    );

                    if (streamOptional.isEmpty())
                        return false;

                    LimitedWithCallbackOutputStream stream = streamOptional.get();

                    Writer writer = new OutputStreamWriter(stream);
                    BufferedWriter bufferedWriter = new BufferedWriter(writer);

                    bufferedWriter.write(text);

                    Optional<String> fileId = filesManager.getRecordFromStream(stream).map(FilesRecord::getId);  // get only fileId, because after close stream record change

                    bufferedWriter.flush();
                    bufferedWriter.close();

                    if (fileId.isEmpty())
                        return false;

                    texts.add(filesManager.getFileRecord(fileId.get()).orElseThrow());
                }

            } catch (IOException e) {
                e.printStackTrace();

                return false;
            }
        }

        boolean result = attachImages(images, contextId);

        if (!result)
            return false;

        result = attachTexts(texts, contextId);

        return result;
    }

    protected boolean attachTexts(List<FilesRecord> files, long contextId) {
        if (!sizeValid(files))
            return false;

        for (FilesRecord record : files) {
            Optional<File> fileOptional = filesManager.getFile(record);

            if (fileOptional.isEmpty())
                return false;

            String data;

            try {
                data = config.fileAttachmentTip.replace("{_CONTENT_}", Files.readString(fileOptional.get().toPath()));
            } catch (IOException e) {
                return false;
            }

            boolean result = pushMessage(contextId, "system", List.of(
                    ContentPart.textContentPart(data)
            ));

            if (!result)
                return false;
        }

        return true;
    }

    protected boolean attachImages(List<FilesRecord> files, long contextId) {
        if (!sizeValid(files))
            return false;

        List<ContentPart> parts = files
                .stream()
                .map((r) -> filesApiRoute + "?fileId=" + r.getId())
                .map(ContentPart::imageUrlContentPart)
                .map((i) -> (ContentPart) i)
                .toList();

        return pushMessage(contextId, "system", parts);
    }

    protected CreateChatCompletionRequest createChatCompletionRequest(boolean includeTools, List<AiMessagesRecord> messages) {
        CreateChatCompletionRequest.Builder builder = CreateChatCompletionRequest.newBuilder();

        if (includeTools)
            builder.tools(toolsList);

        builder.messages(new ChatMessagesBuilder(messages).build())
                .model(config.model)
                .temperature(config.temperature)
                .maxTokens(config.maxTokensPerRequest)
                .topP(config.topP)
                .frequencyPenalty(config.frequencyPenalty)
                .presencePenalty(config.presencePenalty);

        return builder.build();
    }
}

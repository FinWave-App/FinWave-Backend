package app.finwave.backend.api.ai;

import app.finwave.backend.api.ai.content.ContentMeta;
import app.finwave.backend.api.ai.tools.AiTools;
import app.finwave.backend.api.ai.tools.ArrayDeserializer;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.ai.tools.ChatMessagesBuilder;
import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.AiInternalContextUpdate;
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
import java.util.concurrent.ExecutionException;

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

    protected WebSocketWorker webSocketWorker;

    @Inject
    public AiWorker(Configs configs, AiManager manager, AiTools aiTools, WebSocketWorker webSocketWorker) {
        this.config = configs.getState(new AiConfig());
        this.webSocketWorker = webSocketWorker;

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

    protected String ask(long contextId, int userId, UsersSessionsRecord session) {
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

        if (message.toolCalls() != null && runTools(message.toolCalls(), contextId, session)) {
            if (message.content() != null)
                try {
                    webSocketWorker.sendToUser(userId, new AiInternalContextUpdate(contextId, message.content(), "assistant")).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

            return ask(contextId, userId, session);
        }

        return message.content();
    }

    public String ask(long contextId, UsersSessionsRecord session, List<ContentPart> contentParts) {
        log.debug("AI asking in #{} context for user #{}", contextId, session.getUserId());

        if (contentParts != null && !contentParts.isEmpty())
            pushMessage(contextId, "user", contentParts);

        return ask(contextId, session.getUserId(), session);
    }

    public boolean pushMessage(long contextId, String role, List<ContentPart> contentParts) {
        return manager.pushMessage(contextId, role,
                contentToJson(contentParts.stream().map(c -> (Object) c).toList()),
                null
        );
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

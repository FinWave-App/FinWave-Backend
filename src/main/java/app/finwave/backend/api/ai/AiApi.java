package app.finwave.backend.api.ai;

import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.files.FilesManager;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.AiConfig;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.stefanbratanov.jvm.openai.ContentPart;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AiApi {
    protected AiWorker worker;
    protected AiManager manager;
    protected AiConfig config;
    protected FilesManager filesManager;
    protected AiFileWorker aiFileWorker;

    @Inject
    public AiApi(AiWorker worker, AiFileWorker aiFileWorker, AiManager manager, FilesManager filesManager, Configs configs) {
        this.worker = worker;
        this.manager = manager;
        this.config = configs.getState(new AiConfig());

        this.filesManager = filesManager;
        this.aiFileWorker = aiFileWorker;
    }

    public Object newContext(Request request, Response response) {
        if (!config.enabled) {
            response.status(400);

            return ApiMessage.of("AI disabled");
        }

        UsersSessionsRecord sessionRecord = request.attribute("session");

        Optional<String> additionalSystemMessage = ParamsValidator
                .string(request,"additionalSystemMessage")
                .length(1, config.maxAdditionalPrompt)
                .optional();

        Optional<Long> context = worker.initContext(sessionRecord, additionalSystemMessage.orElse(null));

        if (context.isEmpty())
            halt(500);

        response.status(200);

        return new NewContextResponse(context.get());
    }

    public Object attachFile(Request request, Response response) {
        if (!config.enabled) {
            response.status(400);

            return ApiMessage.of("AI disabled");
        }

        UsersSessionsRecord sessionRecord = request.attribute("session");

        long contextId = ParamsValidator
                .integer(request, "contextId")
                .matches((id) -> manager.userOwnContext(sessionRecord.getUserId(), id))
                .require();

        String fileId = ParamsValidator
                .string(request, "fileId")
                .matches((id) -> filesManager.userOwnFile(sessionRecord.getUserId(), id))
                .require();

        FilesRecord record = filesManager.getFileRecord(fileId).orElseThrow();

        boolean result = aiFileWorker.attachFiles(contextId, List.of(record));

        if (!result)
            throw new InvalidParameterException("fileId");

        response.status(200);

        return ApiMessage.of("Attached successfully");
    }

    public Object ask(Request request, Response response) {
        if (!config.enabled) {
            response.status(400);

            return ApiMessage.of("AI disabled");
        }

        UsersSessionsRecord sessionRecord = request.attribute("session");

        long contextId = ParamsValidator
                .longV(request, "contextId")
                .matches((id) -> manager.userOwnContext(sessionRecord.getUserId(), id))
                .require();

        Optional<String> message = ParamsValidator
                .string(request, "message")
                .length(1, config.maxNewMessageSize)
                .optional();

        ArrayList<ContentPart> parts = new ArrayList<>();
        message.ifPresent(s -> parts.add(new ContentPart.TextContentPart(s)));

        String answer = worker.ask(contextId, sessionRecord, parts);

        if (answer == null || answer.isBlank())
            halt(500);

        response.status(200);

        return new AnswerResponse(answer);
    }

    static class AnswerResponse extends ApiResponse {
        public final String answer;

        public AnswerResponse(String answer) {
            this.answer = answer;
        }
    }

    static class NewContextResponse extends ApiResponse {
        public final long contextId;

        public NewContextResponse(long contextId) {
            this.contextId = contextId;
        }
    }
}

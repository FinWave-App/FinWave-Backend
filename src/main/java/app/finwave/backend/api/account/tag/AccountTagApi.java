package app.finwave.backend.api.account.tag;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.AccountsConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.AccountsTagsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AccountTagApi {
    protected AccountsConfig config;
    protected AccountTagDatabase database;
    protected WebSocketWorker socketWorker;

    @Inject
    public AccountTagApi(DatabaseWorker databaseWorker, Configs configs, WebSocketWorker socketWorker) {
        this.database = databaseWorker.get(AccountTagDatabase.class);
        this.config = configs.getState(new AccountsConfig());

        this.socketWorker = socketWorker;
    }

    public Object newTag(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.tags.maxNameLength)
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.tags.maxDescriptionLength)
                .optional();

        if (database.getTagsCount(sessionsRecord.getUserId()) >= config.tags.maxTagsPerUser)
            halt(409);

        Optional<Long> tagId = database.newTag(sessionsRecord.getUserId(), name, description.orElse(null));

        if (tagId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountTags"));

        response.status(201);

        return new NewTagResponse(tagId.get());
    }

    public Object getTags(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<AccountsTagsRecord> records = database.getTags(sessionsRecord.getUserId());

        response.status(200);

        return new GetTagsResponse(records);
    }

    public Object editTagName(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.tags.maxNameLength)
                .require();

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        database.editTagName(tagId, name);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountTags"));

        response.status(200);

        return ApiMessage.of("Tag name edited");
    }

    public Object editTagDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.tags.maxDescriptionLength)
                .optional();

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        database.editTagDescription(tagId, description.orElse(null));

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountTags"));

        response.status(200);

        return ApiMessage.of("Tag description edited");
    }

    public Object deleteTag(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .matches((id) -> database.tagSafeToDelete(id))
                .require();

        database.deleteTag(tagId);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountTags"));

        response.status(200);

        return ApiMessage.of("Tag deleted");
    }

    static class GetTagsResponse extends ApiResponse {
        public final List<Entry> tags;

        public GetTagsResponse(List<AccountsTagsRecord> records) {
            this.tags = records
                    .stream()
                    .map((r) -> new Entry(r.getId(), r.getName(), r.getDescription()))
                    .toList();
        }

        record Entry(long tagId, String name, String description) {}
    }

    static class NewTagResponse extends ApiResponse {
        public final long tagId;

        public NewTagResponse(long tagId) {
            this.tagId = tagId;
        }
    }
}

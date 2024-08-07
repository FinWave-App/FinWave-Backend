package app.finwave.backend.api.transaction.tag;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.TransactionConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.TransactionsTagsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class TransactionTagApi {
    protected TransactionTagDatabase database;
    protected TransactionConfig config;
    protected WebSocketWorker socketWorker;

    @Inject
    public TransactionTagApi(DatabaseWorker databaseWorker, Configs configs, WebSocketWorker socketWorker) {
        this.database = databaseWorker.get(TransactionTagDatabase.class);
        this.config = configs.getState(new TransactionConfig());

        this.socketWorker = socketWorker;
    }

    public Object newTag(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        int type = ParamsValidator
                .integer(request, "type")
                .range(-1,1)
                .require();

        Optional<Long> parentId = ParamsValidator
                .longV(request, "parentId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .optional();

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

        Optional<Long> tagId = database.newTag(sessionsRecord.getUserId(),
                (short) type,
                parentId.orElse(null),
                name,
                description.orElse(null)
        );

        if (tagId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactionTags"));

        response.status(201);

        return new NewTagResponse(tagId.get());
    }

    public Object getTags(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<TransactionsTagsRecord> records = database.getTags(sessionsRecord.getUserId());

        response.status(200);

        return new GetTagsResponse(records);
    }

    public Object editTagType(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        int type = ParamsValidator
                .integer(request, "type")
                .range(-1,1)
                .require();

        database.editTagType(tagId, (short) type);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactionTags"));

        response.status(200);

        return ApiMessage.of("Tag type edited");
    }

    public Object editTagParent(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        Optional<Long> parentId = ParamsValidator
                .longV(request, "parentId")
                .matches((id) -> tagId != id)
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .matches((id) -> database.newParentIsSafe(tagId, id))
                .optional();

        if (parentId.isEmpty()) {
            Boolean toRoot = ParamsValidator
                    .string(request, "setToRoot")
                    .map(Boolean::parseBoolean);

            if (!toRoot) {
                response.status(400);

                return ApiMessage.of("Bad request");
            }

            database.setParentToRoot(tagId);

            socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactionTags"));

            response.status(200);

            return ApiMessage.of("Tag parent edited");
        }

        database.editTagParentId(tagId, parentId.get());

        response.status(200);

        return ApiMessage.of("Tag parent edited");
    }

    public Object editTagName(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.tags.maxNameLength)
                .require();

        database.editTagName(tagId, name);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactionTags"));

        response.status(200);

        return ApiMessage.of("Tag name edited");
    }

    public Object editTagDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.tags.maxDescriptionLength)
                .require();

        database.editTagDescription(tagId, description);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactionTags"));

        response.status(200);

        return ApiMessage.of("Tag description edited");
    }

    static class GetTagsResponse extends ApiResponse {
        public final List<Entry> tags;
        public GetTagsResponse(List<TransactionsTagsRecord> records) {

            this.tags = records
                    .stream()
                    .map((r) -> new Entry(r.getId(), r.getType(), r.getParentsTree().toString(), r.getName(), r.getDescription()))
                    .toList();
        }

        record Entry(long tagId, short type, String parentsTree, String name, String description) {}
    }

    static class NewTagResponse extends ApiResponse {
        public final long tagId;

        public NewTagResponse(long tagId) {
            this.tagId = tagId;
        }
    }
}

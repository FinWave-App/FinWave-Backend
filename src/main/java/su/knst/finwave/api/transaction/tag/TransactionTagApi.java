package su.knst.finwave.api.transaction.tag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.TransactionConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.TransactionsTagsRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class TransactionTagApi {
    protected TransactionTagDatabase database;
    protected TransactionConfig config;

    @Inject
    public TransactionTagApi(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(TransactionTagDatabase.class);
        this.config = configs.getState(new TransactionConfig());
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

        response.status(200);

        database.editTagType(tagId, (short) type);

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

            response.status(200);

            database.setParentToRoot(tagId);

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

        response.status(200);

        database.editTagName(tagId, name);

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

        response.status(200);

        database.editTagDescription(tagId, description);

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

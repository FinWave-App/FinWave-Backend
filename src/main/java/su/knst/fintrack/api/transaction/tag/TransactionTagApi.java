package su.knst.fintrack.api.transaction.tag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Binding;
import org.jooq.postgres.extensions.types.Ltree;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.ApiResponse;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.app.TransactionConfig;
import su.knst.fintrack.http.ApiMessage;
import su.knst.fintrack.jooq.tables.records.TransactionsTagsRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class TransactionTagApi {
    protected TransactionTagDatabase database;
    protected TransactionConfig config;

    @Inject
    public TransactionTagApi(TransactionTagDatabase database, Configs configs) {
        this.database = database;
        this.config = configs.getState(new TransactionConfig());
    }

    public Object newTag(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        int type = ParamsValidator
                .integer(request, "type")
                .range(-1,1)
                .require();

        BigDecimal expectedAmount = ParamsValidator
                .string(request, "expectedAmount")
                .map(BigDecimal::new);

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
                expectedAmount,
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

    public Object editTagExpectedAmount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        BigDecimal expectedAmount = ParamsValidator
                .string(request, "expectedAmount")
                .map(BigDecimal::new);

        response.status(200);

        database.editTagExpectedAmount(tagId, expectedAmount);

        return ApiMessage.of("Tag expected amount edited");
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
                    .map((r) -> new Entry(r.getId(), r.getType(), r.getExpectedAmount(), r.getParentsTree().toString(), r.getName(), r.getDescription()))
                    .toList();
        }

        record Entry(long tagId, short type, BigDecimal expectedAmount, String parentsTree, String name, String description) {}
    }

    static class NewTagResponse extends ApiResponse {
        public final long tagId;

        public NewTagResponse(long tagId) {
            this.tagId = tagId;
        }
    }
}

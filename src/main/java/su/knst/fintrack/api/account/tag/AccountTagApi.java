package su.knst.fintrack.api.account.tag;

import com.google.inject.Inject;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.ApiResponse;
import su.knst.fintrack.api.auth.AuthDatabase;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.app.AccountsConfig;
import su.knst.fintrack.config.general.AuthConfig;
import su.knst.fintrack.jooq.tables.records.AccountsTagsRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

public class AccountTagApi {
    protected AccountsConfig config;
    protected AccountTagDatabase database;

    @Inject
    public AccountTagApi(AccountTagDatabase database, Configs configs) {
        this.database = database;
        this.config = configs.getState(new AccountsConfig());
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

        Optional<Long> tagId = description.isPresent() ?
                database.newTag(sessionsRecord.getUserId(), name, description.get()) :
                database.newTag(sessionsRecord.getUserId(), name);

        if (tagId.isEmpty())
            halt(500);

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
                .longV(request, "noteId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        database.editTagName(tagId, name);

        response.status(200);

        return null;
    }

    public Object editTagDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.tags.maxDescriptionLength)
                .require();

        long tagId = ParamsValidator
                .longV(request, "noteId")
                .matches((id) -> database.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        database.editTagDescription(tagId, description);

        response.status(200);

        return null;
    }

    static class GetTagsResponse extends ApiResponse {
        public final List<Entry> tags;

        public GetTagsResponse(List<AccountsTagsRecord> records) {
            this.tags = records
                    .stream()
                    .map((r) -> new Entry(r.getId(), r.getName(), r.getDescription()))
                    .toList();
        }

        record Entry(long id, String name, String description) {}
    }

    static class NewTagResponse extends ApiResponse {
        public final long tagId;

        public NewTagResponse(long tagId) {
            this.tagId = tagId;
        }
    }
}

package app.finwave.backend.api.category;

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
import app.finwave.backend.jooq.tables.records.CategoriesRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class CategoryApi {
    protected CategoryDatabase database;
    protected TransactionConfig config;
    protected WebSocketWorker socketWorker;

    @Inject
    public CategoryApi(DatabaseWorker databaseWorker, Configs configs, WebSocketWorker socketWorker) {
        this.database = databaseWorker.get(CategoryDatabase.class);
        this.config = configs.getState(new TransactionConfig());

        this.socketWorker = socketWorker;
    }

    public Object newCategory(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        int type = ParamsValidator
                .integer(request, "type")
                .range(-1,1)
                .require();

        Optional<Long> parentId = ParamsValidator
                .longV(request, "parentId")
                .matches((id) -> database.userOwnCategory(sessionsRecord.getUserId(), id))
                .optional();

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.categories.maxNameLength)
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.categories.maxDescriptionLength)
                .optional();

        if (database.getCategoriesCount(sessionsRecord.getUserId()) >= config.categories.maxCategoriesPerUser)
            halt(409);

        Optional<Long> categoryId = database.newCategory(sessionsRecord.getUserId(),
                (short) type,
                parentId.orElse(null),
                name,
                description.orElse(null)
        );

        if (categoryId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("categories"));

        response.status(201);

        return new NewCategoryResponse(categoryId.get());
    }

    public Object getCategories(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<CategoriesRecord> records = database.getCategories(sessionsRecord.getUserId());

        response.status(200);

        return new GetCategoriesResponse(records);
    }

    public Object editCategoryType(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> database.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        int type = ParamsValidator
                .integer(request, "type")
                .range(-1,1)
                .require();

        database.editCategoryType(categoryId, (short) type);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("categories"));

        response.status(200);

        return ApiMessage.of("category type edited");
    }

    public Object editCategoryParent(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> database.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        Optional<Long> parentId = ParamsValidator
                .longV(request, "parentId")
                .matches((id) -> categoryId != id)
                .matches((id) -> database.userOwnCategory(sessionsRecord.getUserId(), id))
                .matches((id) -> database.newParentIsSafe(categoryId, id))
                .optional();

        if (parentId.isEmpty()) {
            Boolean toRoot = ParamsValidator
                    .string(request, "setToRoot")
                    .map(Boolean::parseBoolean);

            if (!toRoot) {
                response.status(400);

                return ApiMessage.of("Bad request");
            }

            database.setParentToRoot(categoryId);

            socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("categories"));

            response.status(200);

            return ApiMessage.of("category parent edited");
        }

        database.editCategoryParentId(categoryId, parentId.get());

        response.status(200);

        return ApiMessage.of("category parent edited");
    }

    public Object editCategoryName(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> database.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.categories.maxNameLength)
                .require();

        database.editCategoryName(categoryId, name);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("categories"));

        response.status(200);

        return ApiMessage.of("category name edited");
    }

    public Object editCategoryDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> database.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.categories.maxDescriptionLength)
                .require();

        database.editCategoryDescription(categoryId, description);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("categories"));

        response.status(200);

        return ApiMessage.of("Category description edited");
    }

    static class GetCategoriesResponse extends ApiResponse {
        public final List<Entry> categories;
        public GetCategoriesResponse(List<CategoriesRecord> records) {

            this.categories = records
                    .stream()
                    .map((r) -> new Entry(r.getId(), r.getType(), r.getParentsTree().toString(), r.getName(), r.getDescription()))
                    .toList();
        }

        record Entry(long categoryId, short type, String parentsTree, String name, String description) {}
    }

    static class NewCategoryResponse extends ApiResponse {
        public final long categoryId;

        public NewCategoryResponse(long categoryId) {
            this.categoryId = categoryId;
        }
    }
}

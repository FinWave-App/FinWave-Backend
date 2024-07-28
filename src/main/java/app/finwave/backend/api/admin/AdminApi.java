package app.finwave.backend.api.admin;

import app.finwave.backend.api.session.SessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.user.UserDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.UsersRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AdminApi {
    protected AdminDatabase database;
    protected UserDatabase userDatabase;
    protected SessionManager sessionManager;

    @Inject
    public AdminApi(DatabaseWorker databaseWorker, SessionManager sessionManager) {
        this.database = databaseWorker.get(AdminDatabase.class);
        this.userDatabase = databaseWorker.get(UserDatabase.class);
        this.sessionManager = sessionManager;
    }

    public Object getUsers(Request request, Response response) {
        int offset = ParamsValidator
                .integer(request, "offset")
                .require();

        int count = ParamsValidator
                .integer(request, "count")
                .require();

        Optional<String> nameFilter = ParamsValidator
                .string(request, "nameFilter")
                .optional();

        List<UsersRecord> records = database.getUserList(offset, count, nameFilter.orElse(null));

        response.status(200);

        return new GetUserListResponse(records);
    }

    public Object getActiveUsersCount(Request request, Response response) {
        response.status(200);

        return new GetCountResponse(database.getActiveUsersCount());
    }

    public Object getUsersCount(Request request, Response response) {
        response.status(200);

        return new GetCountResponse(database.getUsersCount());
    }

    public Object getTransactionsCount(Request request, Response response) {
        response.status(200);

        return new GetCountResponse(database.getTransactionsCount());
    }

    public Object changeUserPassword(Request request, Response response) {
        int userId = ParamsValidator
                .integer(request, "userId")
                .require();

        String password = ParamsValidator
                .string(request, "password")
                .require();

        userDatabase.changeUserPassword(userId, password);
        sessionManager.deleteAllUserSessions(userId);

        response.status(201);

        return ApiMessage.of("Changed");
    }

    public Object registerUser(Request request, Response response) {
        String username = ParamsValidator
                .string(request, "username")
                .require();

        String password = ParamsValidator
                .string(request, "password")
                .require();

        Optional<Integer> userId = userDatabase.registerUser(username, password);

        if (userId.isEmpty())
            halt(500);

        response.status(201);

        return ApiMessage.of("Successful registration");
    }

    static final class GetCountResponse extends ApiResponse {
        public final int count;

        public GetCountResponse(int count) {
            this.count = count;
        }
    }

    static final class GetUserListResponse extends ApiResponse {
        public final List<Entry> users;

        public GetUserListResponse(List<UsersRecord> users) {
            this.users = users.stream()
                    .map((r) -> new Entry(r.getId(), r.getUsername()))
                    .toList();
        }

        record Entry(int id, String username) {}
    }
}

package su.knst.finwave.api.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.account.AccountApi;
import su.knst.finwave.api.session.SessionDatabase;
import su.knst.finwave.api.user.UserDatabase;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.UsersSessions;
import su.knst.finwave.jooq.tables.records.AccountsRecord;
import su.knst.finwave.jooq.tables.records.UsersRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AdminApi {
    protected AdminDatabase database;
    protected UserDatabase userDatabase;
    protected SessionDatabase sessionDatabase;

    @Inject
    public AdminApi(DatabaseWorker databaseWorker) {
        database = databaseWorker.get(AdminDatabase.class);
        userDatabase = databaseWorker.get(UserDatabase.class);
        sessionDatabase = databaseWorker.get(SessionDatabase.class);
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
        sessionDatabase.deleteAllUserSessions(userId);

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

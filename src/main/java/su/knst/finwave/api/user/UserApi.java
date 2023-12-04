package su.knst.finwave.api.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.session.SessionDatabase;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.UserConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.time.ZoneId;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class UserApi {
    protected static final Logger log = LoggerFactory.getLogger(UserApi.class);

    protected UserDatabase database;
    protected SessionDatabase sessionDatabase;

    protected UserConfig config;

    @Inject
    public UserApi(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(UserDatabase.class);
        this.sessionDatabase = databaseWorker.get(SessionDatabase.class);

        this.config = configs.getState(new UserConfig());
    }

    public Object register(Request request, Response response) {
        if (!config.registration.enabled)
            halt(405);

        String login = ParamsValidator.string(request, "login")
                .length(config.minLoginLength, config.maxLoginLength)
                .matches(config.registration.loginRegexFilter)
                .require();

        String password = ParamsValidator.string(request, "password")
                .length(config.minPasswordLength, config.maxPasswordLength)
                .matches(config.registration.passwordRegexFilter)
                .require();

        if (database.userExists(login))
            halt(409);

        Optional<Integer> userId = database.registerUser(login, password);

        if (userId.isEmpty())
            halt(500);

        response.status(201);

        return ApiMessage.of("Successful registration");
    }

    public Object changePassword(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String password = ParamsValidator.string(request, "password")
                .length(config.minPasswordLength, config.maxPasswordLength)
                .matches(config.registration.passwordRegexFilter)
                .require();

        database.changeUserPassword(sessionsRecord.getUserId(), password);
        sessionDatabase.deleteAllUserSessions(sessionsRecord.getUserId());

        response.status(200);

        return ApiMessage.of("Password changed");
    }

    public Object getUsername(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String username = database.getUsername(sessionsRecord.getUserId());

        response.status(200);

        return new GetUsernameResponse(username);
    }

    public Object logout(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        sessionDatabase.deleteSession(sessionsRecord.getId());

        response.status(200);

        return ApiMessage.of("Logged out");
    }

    static class GetUsernameResponse extends ApiResponse {
        public final String username;

        public GetUsernameResponse(String username) {
            this.username = username;
        }
    }
}

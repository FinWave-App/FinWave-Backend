package app.finwave.backend.api.user;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.session.SessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.UserConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.TokenGenerator;
import app.finwave.backend.utils.params.ParamsValidator;

import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class UserApi {
    protected UserDatabase database;
    protected SessionManager sessionManager;

    protected UserConfig config;

    @Inject
    public UserApi(DatabaseWorker databaseWorker, SessionManager sessionManager, Configs configs) {
        this.database = databaseWorker.get(UserDatabase.class);
        this.sessionManager = sessionManager;

        this.config = configs.getState(new UserConfig());
    }

    public Object demoAccount(Request request, Response response) {
        if (!config.demoMode)
            halt(405);

        String login;
        int tries = 0;

        do {
            tries++;
            login = TokenGenerator.generateDemoLogin();

            if (tries > 5)
                halt(500);
        }while (database.userExists(login));

        String password = TokenGenerator.generateDemoPassword();

        Optional<Integer> userId = database.registerUser(login, password);

        if (userId.isEmpty())
            halt(500);

        response.status(201);

        return new DemoAccountResponse(login, password);
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

        if (sessionsRecord.getLimited()) {
            response.status(403);

            return ApiMessage.of("This session is limited");
        }

        String password = ParamsValidator.string(request, "password")
                .length(config.minPasswordLength, config.maxPasswordLength)
                .matches(config.registration.passwordRegexFilter)
                .require();

        database.changeUserPassword(sessionsRecord.getUserId(), password);
        sessionManager.deleteAllUserSessions(sessionsRecord.getUserId());

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

        sessionManager.deleteSession(sessionsRecord);

        response.status(200);

        return ApiMessage.of("Logged out");
    }

    static class GetUsernameResponse extends ApiResponse {
        public final String username;

        public GetUsernameResponse(String username) {
            this.username = username;
        }
    }

    static class DemoAccountResponse extends ApiResponse {
        public final String username;
        public final String password;

        public DemoAccountResponse(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}

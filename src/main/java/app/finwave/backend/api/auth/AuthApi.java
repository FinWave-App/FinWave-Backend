package app.finwave.backend.api.auth;

import app.finwave.backend.api.session.SessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.UserConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.UsersRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.time.LocalDateTime;
import java.util.Optional;

import static spark.Spark.halt;
import static app.finwave.backend.utils.TokenGenerator.generateSessionToken;

@Singleton
public class AuthApi {
    protected UserConfig config;
    protected AuthDatabase database;
    protected SessionManager sessionManager;

    @Inject
    public AuthApi(DatabaseWorker databaseWorker, SessionManager sessionManager, Configs configs) {
        this.database = databaseWorker.get(AuthDatabase.class);
        this.sessionManager = sessionManager;
        this.config = configs.getState(new UserConfig());
    }

    public void auth(Request request, Response response) throws AuthenticationFailException {
        if (request.requestMethod().equals("OPTIONS"))
            return;

        Optional<String> token = ParamsValidator
                .string(request.headers("Authorization"))
                .matches((s) -> s.startsWith("Bearer "))
                .mapOptional((s -> s.replace("Bearer ", "")));

        if (token.isEmpty())
            throw new AuthenticationFailException();

        Optional<UsersSessionsRecord> sessionsRecord = sessionManager.auth(token.get());

        if (sessionsRecord.isEmpty())
            throw new AuthenticationFailException();

        LocalDateTime now = LocalDateTime.now();

        if (sessionsRecord.get().getExpiresAt().isBefore(now)) {
            sessionManager.deleteSession(sessionsRecord.get());

            throw new AuthenticationFailException();
        }

        if (now.plusDays(config.userSessionsLifetimeDays - 1).isAfter(sessionsRecord.get().getExpiresAt()))
            sessionManager.updateSessionLifetime(sessionsRecord.get());

        request.attribute("session", sessionsRecord.get());
    }

    public void authAdmin(Request request, Response response) throws AuthenticationFailException {
        if (request.requestMethod().equals("OPTIONS"))
            return;

        auth(request, response);

        UsersSessionsRecord record = request.attribute("session");

        if (record == null || record.getUserId() != 1)
            throw new AuthenticationFailException();
    }

    public Object login(Request request, Response response) {
        String login = ParamsValidator
                .string(request, "login")
                .length(config.minLoginLength, config.maxLoginLength)
                .require();

        String password = ParamsValidator
                .string(request, "password")
                .length(config.minPasswordLength, config.maxPasswordLength)
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxSessionDescriptionLength)
                .optional();

        Optional<UsersRecord> usersRecord = database.authUser(login, password);

        if (usersRecord.isEmpty())
            halt(401);

        if (config.demoMode && usersRecord.get().getId() == 1)
            halt(401);

        Optional<UsersSessionsRecord> session = sessionManager.newSession(usersRecord.get().getId(), config.userSessionsLifetimeDays, description.orElse(null), false);

        if (session.isEmpty())
            halt(500);

        return new LoginResponse(session.get().getToken(), config.userSessionsLifetimeDays);
    }

    static class LoginResponse extends ApiResponse {
        public final String token;
        public final int lifetimeDays;

        public LoginResponse(String token, int lifetimeDays) {
            this.token = token;
            this.lifetimeDays = lifetimeDays;
        }
    }
}

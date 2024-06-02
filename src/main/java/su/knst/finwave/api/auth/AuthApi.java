package su.knst.finwave.api.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.session.SessionDatabase;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.UserConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.UsersRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.time.LocalDateTime;
import java.util.Optional;

import static spark.Spark.halt;
import static su.knst.finwave.utils.TokenGenerator.generateSessionToken;

@Singleton
public class AuthApi {
    protected UserConfig config;
    protected AuthDatabase database;
    protected SessionDatabase sessionDatabase;

    @Inject
    public AuthApi(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(AuthDatabase.class);
        this.sessionDatabase = databaseWorker.get(SessionDatabase.class);
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

        Optional<UsersSessionsRecord> sessionsRecord = database.authUser(token.get());

        if (sessionsRecord.isEmpty())
            throw new AuthenticationFailException();

        LocalDateTime now = LocalDateTime.now();

        if (sessionsRecord.get().getExpiresAt().isBefore(now)) {
            sessionDatabase.deleteSession(sessionsRecord.get().getToken());

            throw new AuthenticationFailException();
        }

        if (now.plusDays(config.userSessionsLifetimeDays - 1).isAfter(sessionsRecord.get().getExpiresAt()))
            sessionDatabase.updateSessionLifetime(sessionsRecord.get().getToken(), config.userSessionsLifetimeDays);

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

        Optional<UsersRecord> sessionsRecord = database.authUser(login, password);

        if (sessionsRecord.isEmpty())
            halt(401);

        if (config.demoMode && sessionsRecord.get().getId() == 1)
            halt(401);

        String token = generateSessionToken();

        sessionDatabase.newSession(sessionsRecord.get().getId(), token, config.userSessionsLifetimeDays, description.orElse(null), false);

        return new LoginResponse(token, config.userSessionsLifetimeDays);
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

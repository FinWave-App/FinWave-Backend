package su.knst.fintrack.api.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.ApiResponse;
import su.knst.fintrack.api.auth.AuthDatabase;
import su.knst.fintrack.api.transaction.TransactionApi;
import su.knst.fintrack.api.transaction.filter.TransactionsFilter;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.general.UserConfig;
import su.knst.fintrack.http.ApiMessage;
import su.knst.fintrack.jooq.tables.records.TransactionsRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.fintrack.utils.SessionGenerator.generateSessionToken;

@Singleton
public class SessionApi {
    protected SessionDatabase database;
    protected UserConfig config;

    @Inject
    public SessionApi(SessionDatabase database, Configs configs) {
        this.database = database;
        this.config = configs.getState(new UserConfig());
    }

    public Object newSession(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        int lifetimeDays = ParamsValidator
                .integer(request, "lifetimeDays")
                .range(1, Integer.MAX_VALUE)
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxSessionDescriptionLength)
                .optional();

        String token = generateSessionToken();

        database.newSession(sessionRecord.getUserId(), token, lifetimeDays, description.orElse(null));

        response.status(200);

        return new NewSessionResponse(token);
    }

    public Object getSessions(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        List<UsersSessionsRecord> records = database.getUserSessions(sessionRecord.getUserId());

        response.status(200);

        return new GetSessionsResponse(records);
    }

    public Object deleteSession(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        long sessionId = ParamsValidator
                .integer(request, "sessionId")
                .matches((id) -> database.userOwnSession(sessionRecord.getUserId(), id))
                .require();

        database.deleteSession(sessionId);

        response.status(200);

        return ApiMessage.of("Session deleted");
    }

    static class GetSessionsResponse extends ApiResponse {
        public final List<Entry> sessions;

        public GetSessionsResponse(List<UsersSessionsRecord> sessions) {
            this.sessions = sessions.stream()
                    .map(r -> new Entry(
                            r.getId(),
                            r.getToken(),
                            r.getCreatedAt(),
                            r.getExpiresAt(),
                            r.getDescription()))
                    .toList();
        }

        record Entry(long sessionId, String token, LocalDateTime createdAt, LocalDateTime expiresAt, String description) {}
    }

    static class NewSessionResponse extends ApiResponse {
        public final String token;

        public NewSessionResponse(String token) {
            this.token = token;
        }
    }
}

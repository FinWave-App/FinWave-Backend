package su.knst.finwave.api.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.UserConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.finwave.utils.SessionGenerator.generateSessionToken;

@Singleton
public class SessionApi {
    protected SessionDatabase database;
    protected UserConfig config;

    @Inject
    public SessionApi(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(SessionDatabase.class);
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

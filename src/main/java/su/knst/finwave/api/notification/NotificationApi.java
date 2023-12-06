package su.knst.finwave.api.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.notification.data.NotificationOptions;
import su.knst.finwave.api.notification.data.point.WebPushPointData;
import su.knst.finwave.api.session.SessionApi;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.NotificationsConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.NotificationsPointsRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class NotificationApi {
    protected NotificationDatabase database;
    protected NotificationsConfig config;

    @Inject
    public NotificationApi(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(NotificationDatabase.class);
        this.config = configs.getState(new NotificationsConfig());
    }

    public Object registerNewWebPushPoint(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        String endpoint = ParamsValidator
                .string(request, "endpoint")
                .length(1, config.webPush.maxEndpointLength)
                .require();

        String auth = ParamsValidator
                .string(request, "auth")
                .length(1, config.webPush.maxAuthLength)
                .require();

        String p256dh = ParamsValidator
                .string(request, "p256dh")
                .length(1, config.webPush.maxP256dhLength)
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .require();

        boolean isPrimary = ParamsValidator
                .string(request, "primary")
                .mapOptional(s -> s.equals("true"))
                .orElse(false);

        if (database.getPointsCount(sessionRecord.getUserId()) >= config.maxPointsPerUser)
            halt(409);

        Optional<Long> pointId = database.registerNotificationPoint(
                sessionRecord.getUserId(),
                isPrimary,
                new WebPushPointData(endpoint, auth, p256dh), description
        );

        if (pointId.isEmpty())
            halt(500);

        response.status(200);

        return new NewPointResponse(pointId.get());
    }

    public Object getPoints(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<NotificationsPointsRecord> list = database.getUserNotificationsPoints(sessionsRecord.getUserId());

        response.status(200);

        return new GetPointsResponse(list);
    }

    public Object editPointDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long pointId = ParamsValidator
                .longV(request, "pointId")
                .matches((id) -> database.userOwnPoint(sessionsRecord.getUserId(), id))
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .require();

        database.editNotificationPointDescription(pointId, description);

        response.status(200);

        return ApiMessage.of("Point description edited");
    }

    public Object editPointPrimary(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long pointId = ParamsValidator
                .longV(request, "pointId")
                .matches((id) -> database.userOwnPoint(sessionsRecord.getUserId(), id))
                .require();

        boolean isPrimary = ParamsValidator
                .string(request, "primary")
                .mapOptional(s -> s.equals("true"))
                .orElse(false);

        database.editNotificationPointPrimary(pointId, isPrimary);

        response.status(200);

        return ApiMessage.of("Point primary edited");
    }

    public Object deletePoint(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long pointId = ParamsValidator
                .longV(request, "pointId")
                .matches((id) -> database.userOwnPoint(sessionsRecord.getUserId(), id))
                .require();

        database.deleteNotificationPoint(pointId);

        response.status(200);

        return ApiMessage.of("Point deleted");
    }

    public Object pushNotification(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long pointId = ParamsValidator
                .longV(request, "pointId")
                .matches((id) -> database.userOwnPoint(sessionsRecord.getUserId(), id))
                .optional()
                .orElse(-1L);

        String text = ParamsValidator
                .string(request, "text")
                .length(1, config.maxNotificationLength)
                .require();

        boolean silent = ParamsValidator
                .string(request, "silent")
                .mapOptional(s -> s.equals("true"))
                .orElse(false);

        database.pushNotification(sessionsRecord.getUserId(), text, new NotificationOptions(silent, pointId));

        response.status(200);

        return ApiMessage.of("Notification pushed");
    }

    static class GetPointsResponse extends ApiResponse {
        public final List<Entry> sessions;

        public GetPointsResponse(List<NotificationsPointsRecord> sessions) {
            this.sessions = sessions.stream()
                    .map(r -> new Entry(
                            r.getId(),
                            r.getIsPrimary(),
                            r.getType(),
                            r.getCreatedAt(),
                            r.getDescription()))
                    .toList();
        }

        record Entry(long pointId, boolean isPrimary, short type, OffsetDateTime createdAt, String description) {}
    }

    static class NewPointResponse extends ApiResponse {
        public final long pointId;

        public NewPointResponse(long pointId) {
            this.pointId = pointId;
        }
    }
}

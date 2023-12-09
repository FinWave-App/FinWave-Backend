package su.knst.finwave.api.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jose4j.base64url.Base64Url;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.notification.data.Notification;
import su.knst.finwave.api.notification.data.NotificationOptions;
import su.knst.finwave.api.notification.data.point.WebPushPointData;
import su.knst.finwave.api.notification.manager.NotificationManager;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.NotificationsConfig;
import su.knst.finwave.config.general.VapidKeysConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.NotificationsPointsRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class NotificationApi {
    protected NotificationDatabase database;
    protected NotificationManager manager;
    protected NotificationsConfig config;
    protected String vapidPublicKey;

    @Inject
    public NotificationApi(DatabaseWorker databaseWorker, NotificationManager manager, Configs configs) {
        this.database = databaseWorker.get(NotificationDatabase.class);
        this.manager = manager;
        this.config = configs.getState(new NotificationsConfig());
        this.vapidPublicKey = configs.getState(new VapidKeysConfig()).publicKey;
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
                .matches(s -> Base64Url.decode(s)[0] == 0x04)
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

        HashMap<String, String> args = new HashMap<>();
        args.put("icon", "/icon.png");
        args.put("silent", String.valueOf(silent));

        manager.push(Notification.create(
                text,
                new NotificationOptions(silent, pointId, args),
                sessionsRecord.getUserId()
        ));

        response.status(200);

        return ApiMessage.of("Notification pushed");
    }

    public Object getKey(Request request, Response response) {
        response.status(200);

        return new VapidKeyResponse(vapidPublicKey);
    }

    static class GetPointsResponse extends ApiResponse {
        public final List<Entry> points;

        public GetPointsResponse(List<NotificationsPointsRecord> sessions) {
            this.points = sessions.stream()
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

    static class VapidKeyResponse extends ApiResponse {
        public final String publicKey;

        public VapidKeyResponse(String publicKey) {
            this.publicKey = publicKey;
        }
    }
}

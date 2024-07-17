package app.finwave.backend.api.event;

import app.finwave.backend.api.event.messages.RequestMessage;
import app.finwave.backend.api.event.messages.ResponseMessage;
import app.finwave.backend.api.event.messages.requests.AuthMessageBody;
import app.finwave.backend.api.event.messages.requests.NewNotificationPointBody;
import app.finwave.backend.api.event.messages.requests.SubscribeNotificationsBody;
import app.finwave.backend.api.event.messages.response.auth.AuthStatus;
import app.finwave.backend.api.event.messages.response.GenericResponse;
import app.finwave.backend.api.event.messages.response.notifications.NotificationPointRegistered;
import app.finwave.backend.api.event.messages.response.notifications.NotificationSubscribeResponse;
import app.finwave.backend.api.notification.NotificationDatabase;
import app.finwave.backend.api.notification.data.point.WebSocketPointData;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.NotificationsConfig;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

import static app.finwave.backend.api.ApiResponse.GSON;
import static app.finwave.backend.api.notification.data.point.NotificationPointType.WEB_SOCKET;

public class WebSocketClient {
    protected WebSocketWorker worker;
    protected Session session;
    protected RemoteEndpoint remote;
    protected int userId = -1;

    protected NotificationDatabase notificationDatabase;

    protected NotificationsConfig notificationsConfig;

    public WebSocketClient(Session session, NotificationDatabase notificationDatabase, WebSocketWorker worker, Configs configs) {
        this.session = session;
        this.remote = session.getRemote();

        this.notificationDatabase = notificationDatabase;
        this.worker = worker;

        this.notificationsConfig = configs.getState(new NotificationsConfig());
    }

    public void onMessage(String rawMessage) throws IOException {
        if (rawMessage.equals("ping")) {
            send("pong");

            return;
        }

        RequestMessage message;

        try {
            message = GSON.fromJson(rawMessage, RequestMessage.class);
        } catch (JsonSyntaxException e) {
            send(new GenericResponse("Invalid request", 1));

            return;
        }

        try {
            switch (message.type) {
                case "auth" -> auth(message.getBody(AuthMessageBody.class));
                case "newNotification" -> newNotificationPoint(message.getBody(NewNotificationPointBody.class));
                case "subscribeNotification" -> subscribeNotifications(message.getBody(SubscribeNotificationsBody.class));
                default -> send(new GenericResponse("Invalid type"));
            }
        }catch (Exception e) {
            send(new GenericResponse("Server error", 1));
        }
    }

    protected void subscribeNotifications(SubscribeNotificationsBody body) throws IOException {
        if (userId == -1) {
            send(new AuthStatus("Unauthorized"));

            return;
        }

        if (body.pointUUID == null) {
            send(new GenericResponse("Invalid request", 1));

            return;
        }

        var userPoints = notificationDatabase.getUserNotificationsPoints(userId);

        Optional<WebSocketPointData> pointData = userPoints.stream()
                .filter(p -> p.getType() == WEB_SOCKET.ordinal())
                .map(p -> GSON.fromJson(p.getData().data(), WebSocketPointData.class))
                .filter(d -> d.uuid.equals(body.pointUUID))
                .findAny();

        if (pointData.isEmpty()) {
            send(new GenericResponse("Invalid UUID", 1));

            return;
        }

        boolean result = worker.subscribeNotification(this, body.pointUUID);

        send(new NotificationSubscribeResponse(result ? "Subscribed" : "Failed"));
    }

    protected void newNotificationPoint(NewNotificationPointBody body) throws IOException {
        if (userId == -1) {
            send(new AuthStatus("Unauthorized"));

            return;
        }

        String description = ParamsValidator.string(body.description)
                .length(1, notificationsConfig.maxDescriptionLength)
                .require();

        UUID newUUID = UUID.randomUUID();

        Optional<Long> pointId = notificationDatabase.registerNotificationPoint(
                userId,
                body.isPrimary,
                new WebSocketPointData(newUUID), description
        );

        if (pointId.isEmpty()) {
            send(new GenericResponse("Server error", 1));

            return;
        }

        send(new NotificationPointRegistered(pointId.get(), newUUID));
    }

    protected void auth(AuthMessageBody body) throws IOException {
        if (userId != -1) {
            send(new AuthStatus("Authorized already"));

            return;
        }

        Optional<UsersSessionsRecord> record = worker.authClient(this, body.token);

        if (record.isEmpty()) {
            send(new AuthStatus("Failed"));

            return;
        }

        this.userId = record.get().getUserId();

        send(new AuthStatus("Successful"));
    }

    public Future<Void> send(ResponseMessage<?> message) throws IOException {
        return send(GSON.toJson(message));
    }

    protected Future<Void> send(String jsonMessage) throws IOException {
        return remote.sendStringByFuture(jsonMessage);
    }
}

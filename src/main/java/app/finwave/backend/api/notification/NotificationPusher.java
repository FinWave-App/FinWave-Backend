package app.finwave.backend.api.notification;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.notification.data.point.WebSocketPointData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import nl.martijndwars.webpush.PushAsyncService;
import org.asynchttpclient.Response;
import org.jose4j.lang.JoseException;
import app.finwave.backend.api.notification.data.Notification;
import app.finwave.backend.api.notification.data.point.NotificationPointType;
import app.finwave.backend.api.notification.data.point.WebPushPointData;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.VapidKeysConfig;
import app.finwave.backend.jooq.tables.records.NotificationsPointsRecord;
import app.finwave.backend.utils.VapidGenerator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

import static app.finwave.backend.api.ApiResponse.GSON;

@Singleton
public class NotificationPusher {
    protected PushAsyncService pushService;
    protected WebSocketWorker socketWorker;

    @Inject
    public NotificationPusher(Configs configs, WebSocketWorker socketWorker) throws GeneralSecurityException {
        VapidKeysConfig config = configs.getState(new VapidKeysConfig());

        this.socketWorker = socketWorker;

        this.pushService = new PushAsyncService(new KeyPair(
                VapidGenerator.stringToPublicKey(config.publicKey),
                VapidGenerator.stringToPrivateKey(config.privateKey)
        ));
    }

    public CompletableFuture<Boolean> push(NotificationsPointsRecord pointRecord, Notification notification) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        switch (NotificationPointType.values()[(int)pointRecord.getType()]){
            case WEB_PUSH -> {
                pushWeb(pointRecord, notification)
                        .whenComplete((r, t) -> result.complete(t == null));
            }
            case WEB_SOCKET -> {
                pushSocket(pointRecord, notification)
                        .whenComplete((r, t) -> result.complete(t == null));
            }
            default -> result.complete(false);
        }

        return result;
    }

    protected CompletableFuture<Boolean> pushSocket(NotificationsPointsRecord pointRecord, Notification notification) {
        WebSocketPointData data = GSON.fromJson(pointRecord.getData().data(), WebSocketPointData.class);

        return socketWorker.sendNotification(data.uuid, notification);
    }

    protected CompletableFuture<Response> pushWeb(NotificationsPointsRecord pointRecord, Notification notification) {
        WebPushPointData data = GSON.fromJson(pointRecord.getData().data(), WebPushPointData.class);

        try {
            return pushService.send(new nl.martijndwars.webpush.Notification(
                    data.endpoint,
                    data.p256dh,
                    data.auth,
                    GSON.toJson(notification)
            ));
        } catch (GeneralSecurityException | IOException | JoseException e) {
            e.printStackTrace();

            return CompletableFuture.failedFuture(e);
        }
    }
}

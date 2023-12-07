package su.knst.finwave.service.notifications;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import su.knst.finwave.api.notification.data.Notification;
import su.knst.finwave.api.notification.data.point.NotificationPointType;
import su.knst.finwave.api.notification.data.point.WebPushPointData;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.VapidKeysConfig;
import su.knst.finwave.jooq.tables.records.NotificationsPointsRecord;
import su.knst.finwave.utils.VapidGenerator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
import java.util.concurrent.ExecutionException;

import static su.knst.finwave.api.ApiResponse.GSON;

@Singleton
public class NotificationPusher {
    protected PushService pushService;

    @Inject
    public NotificationPusher(Configs configs) throws GeneralSecurityException {
        VapidKeysConfig config = configs.getState(new VapidKeysConfig());

        this.pushService = new PushService(new KeyPair(
                VapidGenerator.stringToPublicKey(config.publicKey),
                VapidGenerator.stringToPrivateKey(config.privateKey)
        ));
    }

    public boolean push(NotificationsPointsRecord pointRecord, Notification notification) {
        switch (NotificationPointType.values()[(int)pointRecord.getType()]){
            case WEB_PUSH -> {
                return pushWeb(pointRecord, notification);
            }
            default -> {
                return false;
            }
        }
    }

    protected boolean pushWeb(NotificationsPointsRecord pointRecord, Notification notification) {
        WebPushPointData data = GSON.fromJson(pointRecord.getData().data(), WebPushPointData.class);

        try {
            pushService.send(new nl.martijndwars.webpush.Notification(
                    data.endpoint,
                    data.p256dh,
                    data.auth,
                    GSON.toJson(notification)
            ));
        } catch (GeneralSecurityException | IOException | JoseException | ExecutionException | InterruptedException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }
}

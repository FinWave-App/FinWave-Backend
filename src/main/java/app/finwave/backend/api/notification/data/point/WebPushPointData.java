package app.finwave.backend.api.notification.data.point;

public class WebPushPointData extends AbstractNotificationPointData {
    public final String endpoint;
    public final String auth;
    public final String p256dh;

    public WebPushPointData(String endpoint, String auth, String p256dh) {
        super(NotificationPointType.WEB_PUSH);
        this.endpoint = endpoint;
        this.auth = auth;
        this.p256dh = p256dh;
    }
}

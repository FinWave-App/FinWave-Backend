package su.knst.finwave.api.notification.data.point;

public abstract class AbstractNotificationPointData {
    public final transient NotificationPointType type;

    protected AbstractNotificationPointData(NotificationPointType type) {
        this.type = type;
    }
}

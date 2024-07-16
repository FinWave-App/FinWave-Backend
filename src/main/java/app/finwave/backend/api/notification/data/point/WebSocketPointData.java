package app.finwave.backend.api.notification.data.point;

import java.util.UUID;

public class WebSocketPointData extends AbstractNotificationPointData {
    public final UUID uuid;

    public WebSocketPointData(UUID uuid) {
        super(NotificationPointType.WEB_SOCKET);

        this.uuid = uuid;
    }
}

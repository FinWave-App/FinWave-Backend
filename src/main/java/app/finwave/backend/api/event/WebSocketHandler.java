package app.finwave.backend.api.event;

import app.finwave.backend.Main;
import app.finwave.backend.api.notification.NotificationDatabase;
import app.finwave.backend.config.Configs;
import app.finwave.backend.database.DatabaseWorker;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

@WebSocket
public class WebSocketHandler {
    protected WebSocketWorker worker;
    protected NotificationDatabase notificationDatabase;
    protected Configs configs;

    protected HashMap<Session, WebSocketClient> clients = new HashMap<>();

    protected ReentrantLock lock = new ReentrantLock();

    public WebSocketHandler() {
        worker = Main.INJ.getInstance(WebSocketWorker.class);
        notificationDatabase = Main.INJ.getInstance(DatabaseWorker.class).get(NotificationDatabase.class);
        configs = Main.INJ.getInstance(Configs.class);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        session.setIdleTimeout(60000);
        WebSocketClient client = new WebSocketClient(session, notificationDatabase, worker, configs);
        lock.lock();

        try {
            clients.put(session, client);
        }finally {
            lock.unlock();
        }

        worker.registerAnonClient(client);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        WebSocketClient client;
        lock.lock();

        try {
            client = clients.remove(session);
        }finally {
            lock.unlock();
        }

        if (client.userId == -1) {
            worker.removeAnonClient(client);

            return;
        }

        worker.removeAuthedClient(client, client.userId);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String rawMessage) throws IOException {
        clients.get(session).onMessage(rawMessage);
    }


}

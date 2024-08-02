package app.finwave.backend.api.event;

import app.finwave.backend.api.auth.AuthDatabase;
import app.finwave.backend.api.event.messages.ResponseMessage;
import app.finwave.backend.api.event.messages.response.notifications.NotificationEvent;
import app.finwave.backend.api.notification.data.Notification;
import app.finwave.backend.api.session.SessionManager;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static app.finwave.backend.api.ApiResponse.GSON;

@Singleton
public class WebSocketWorker {
    protected HashSet<WebSocketClient> anonClients = new HashSet<>();
    protected HashMap<Integer, HashSet<WebSocketClient>> authedClients = new HashMap<>();

    protected HashMap<UUID, WebSocketClient> notificationSubscribes = new HashMap<>();
    protected HashMap<WebSocketClient, UUID> reversedNotificationSubscribes = new HashMap<>();

    protected ReentrantLock anonLock = new ReentrantLock();
    protected ReentrantLock authedLock = new ReentrantLock();
    protected ReentrantLock notificationLock = new ReentrantLock();

    protected HashMap<Integer, ReentrantLock> authedLocks = new HashMap<>();

    protected SessionManager sessionManager;

    protected ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    public WebSocketWorker(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void registerAnonClient(WebSocketClient client) {
        anonLock.lock();

        try {
            anonClients.add(client);
        }finally {
            anonLock.unlock();
        }
    }

    public void removeAnonClient(WebSocketClient client) {
        anonLock.lock();

        try {
            anonClients.remove(client);
        }finally {
            anonLock.unlock();
        }
    }

    public void removeAuthedClient(WebSocketClient client, int userId) {
        ReentrantLock setLock = getAuthedLock(userId);

        authedLock.lock();
        setLock.lock();

        try {
            authedClients.computeIfAbsent(userId, k -> new HashSet<>()).remove(client);
        }finally {
            authedLock.unlock();
            setLock.unlock();
        }

        notificationLock.lock();

        try {
            UUID removed = reversedNotificationSubscribes.remove(client);
            notificationSubscribes.remove(removed);
        }finally {
            notificationLock.unlock();
        }
    }

    public Optional<UsersSessionsRecord> authClient(WebSocketClient client, String token) {
        Optional<UsersSessionsRecord> record = sessionManager.auth(token);

        if (record.isEmpty())
            return Optional.empty();

        int userId = record.get().getUserId();

        ReentrantLock setLock = getAuthedLock(userId);

        anonLock.lock();
        authedLock.lock();
        setLock.lock();

        try {
            anonClients.remove(client);
            authedClients.computeIfAbsent(userId, k -> new HashSet<>()).add(client);
        }finally {
            setLock.unlock();
            authedLock.unlock();
            anonLock.unlock();
        }

        return record;
    }

    public boolean subscribeNotification(WebSocketClient client, UUID pointUUID) {
        notificationLock.lock();

        try {
            if (notificationSubscribes.containsKey(pointUUID))
                return false;

            notificationSubscribes.put(pointUUID, client);
            reversedNotificationSubscribes.put(client, pointUUID);

            return true;
        }finally {
            notificationLock.unlock();
        }
    }

    protected HashSet<WebSocketClient> getUserClients(int userId) {
        authedLock.lock();

        try {
            return authedClients.computeIfAbsent(userId, k -> new HashSet<>());
        }finally {
            authedLock.unlock();
        }
    }

    protected ReentrantLock getAuthedLock(int userId) {
        authedLock.lock();

        try {
            return authedLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        }finally {
            authedLock.unlock();
        }
    }

    public CompletableFuture<Boolean> sendNotification(UUID uuid, Notification notification) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        executor.submit(() -> {
            boolean taskResult = false;
            WebSocketClient client;

            notificationLock.lock();
            try {
                if (!notificationSubscribes.containsKey(uuid)) {
                    result.complete(taskResult);

                    return;
                }

                client = notificationSubscribes.get(uuid);
            }finally {
                notificationLock.unlock();
            }

            try {
                if (client != null) {
                    client.send(new NotificationEvent(notification)).get();

                    taskResult = true;
                }
            }catch (Exception ignored) {}


            result.complete(taskResult);
        });

        return result;
    }

    public CompletableFuture<Boolean> sendToUser(int userId, ResponseMessage<?> message) {
        ReentrantLock lock = getAuthedLock(userId);
        HashSet<WebSocketClient> clients = getUserClients(userId);
        String rawMessage = GSON.toJson(message);
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        CompletableFuture<Boolean>[] tasks = new CompletableFuture[clients.size()];
        AtomicBoolean anyoneSuccess = new AtomicBoolean(false);
        int i = 0;

        lock.lock();

        try {
            if (clients.isEmpty())
                return CompletableFuture.completedFuture(false);

            for (WebSocketClient client : clients) {
                CompletableFuture<Boolean> future = new CompletableFuture<>();

                future.whenComplete((r, t) -> {
                    if (t != null || !r)
                        return;

                    anyoneSuccess.compareAndSet(false, true);
                });

                tasks[i] = future;

                executor.submit(() -> {
                    boolean taskResult = false;

                    try {
                        client.send(rawMessage).get();
                        taskResult = true;
                    } catch (Exception ignored) {

                    }

                    future.complete(taskResult);
                });

                i++;
            }
        }finally {
            lock.unlock();
        }

        CompletableFuture.allOf(tasks).whenComplete((r, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
                return;
            }

            result.complete(anyoneSuccess.get());
        });

        return result;
    }
}

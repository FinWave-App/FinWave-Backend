package app.finwave.backend.api.session;

import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.CachingConfig;
import app.finwave.backend.config.general.UserConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.CacheHandyBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static app.finwave.backend.utils.TokenGenerator.generateSessionToken;

@Singleton
public class SessionManager {
    protected SessionDatabase database;
    protected UserConfig config;

    protected LoadingCache<Integer, List<UsersSessionsRecord>> listCache;
    protected LoadingCache<String, Optional<UsersSessionsRecord>> tokenCache;

    @Inject
    public SessionManager(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(SessionDatabase.class);
        this.config = configs.getState(new UserConfig());

        CachingConfig.Sessions cacheConfig = configs.getState(new CachingConfig()).sessions;

        this.tokenCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cacheConfig.maxTokens,
                (t) -> database.get(t)
        );

        this.listCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cacheConfig.maxLists,
                (userId) -> {
                    var result = database.getUserSessions(userId);

                    tokenCache.putAll(
                            result.stream().collect(Collectors.toMap(UsersSessionsRecord::getToken, Optional::of))
                    );

                    return result;
                }
        );
    }

    public Optional<UsersSessionsRecord> auth(String token) {
        try {
            return tokenCache.get(token);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<UsersSessionsRecord> newSession(int userId, int lifetimeDays, String description, boolean limited) {
        String token = generateSessionToken();

        Optional<UsersSessionsRecord> record = database.newSession(userId, token, lifetimeDays, description, limited);

        if (record.isEmpty())
            return Optional.empty();

        listCache.invalidate(userId);
        tokenCache.put(token, record);

        return record;
    }

    public List<UsersSessionsRecord> getSessions(int userId) {
        try {
            return listCache.get(userId);
        } catch (ExecutionException e) {
            e.printStackTrace();

            return List.of();
        }
    }

    public void deleteSession(UsersSessionsRecord record) {
        database.deleteSession(record.getId());

        listCache.invalidate(record.getUserId());
        tokenCache.invalidate(record.getToken());
    }

    public void deleteSession(long sessionId) {
        UsersSessionsRecord removed = database.deleteSession(sessionId);

        listCache.invalidate(removed.getUserId());
        tokenCache.invalidate(removed.getToken());
    }

    public void deleteAllUserSessions(int userId) {
        List<UsersSessionsRecord> removed = database.deleteAllUserSessions(userId);

        tokenCache.invalidateAll(removed.stream()
                .map(UsersSessionsRecord::getToken)
                .collect(Collectors.toList())
        );

        listCache.invalidate(userId);
    }

    public void deleteOverdueSessions() {
        List<UsersSessionsRecord> removed = database.deleteOverdueSessions();

        tokenCache.invalidateAll(removed.stream()
                .map(UsersSessionsRecord::getToken)
                .collect(Collectors.toList())
        );

        listCache.invalidateAll(removed.stream()
                .map(UsersSessionsRecord::getUserId)
                .collect(Collectors.toSet())
        );
    }

    public void updateSessionLifetime(long sessionId, int userId) {
        UsersSessionsRecord record = database.updateSessionLifetime(sessionId, config.userSessionsLifetimeDays);

        listCache.invalidate(userId);
        tokenCache.put(record.getToken(), Optional.of(record));
    }

    public void updateSessionLifetime(UsersSessionsRecord record) {
        updateSessionLifetime(record.getId(), record.getUserId());
    }

    public boolean userOwnSession(int userId, long sessionId) {
        try {
            return listCache.get(userId).stream().anyMatch((r) -> r.getId() == sessionId);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return false;
    }
}

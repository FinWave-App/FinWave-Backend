package app.finwave.backend.api.session;

import org.jooq.DSLContext;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.USERS_SESSIONS;

public class SessionDatabase extends AbstractDatabase {

    public SessionDatabase(DSLContext context) {
        super(context);
    }

    public Optional<UsersSessionsRecord> newSession(int userId, String token, int lifetimeDays, String description, boolean limited) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(lifetimeDays);

        return context.insertInto(USERS_SESSIONS)
                .set(USERS_SESSIONS.USER_ID, userId)
                .set(USERS_SESSIONS.TOKEN, token)
                .set(USERS_SESSIONS.CREATED_AT, now)
                .set(USERS_SESSIONS.EXPIRES_AT, expires)
                .set(USERS_SESSIONS.DESCRIPTION, description)
                .set(USERS_SESSIONS.LIMITED, limited)
                .returning()
                .fetchOptional();
    }

    public Optional<UsersSessionsRecord> get(String sessionToken) {
        return context.selectFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.TOKEN.eq(sessionToken))
                .fetchOptional();
    }

    public UsersSessionsRecord updateSessionLifetime(long sessionId, int lifetimeDays) {
        return context.update(USERS_SESSIONS)
                .set(USERS_SESSIONS.EXPIRES_AT, LocalDateTime.now().plusDays(lifetimeDays))
                .where(USERS_SESSIONS.ID.eq(sessionId))
                .returning()
                .fetchOptional()
                .orElse(null);
    }

    public UsersSessionsRecord deleteSession(long sessionId) {
        return context.deleteFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.ID.eq(sessionId))
                .returning()
                .fetchOptional()
                .orElse(null);
    }

    public List<UsersSessionsRecord> deleteAllUserSessions(int userId) {
        return context.deleteFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.USER_ID.eq(userId))
                .returning()
                .fetch();
    }

    public List<UsersSessionsRecord> getUserSessions(int userId) {
        return context.selectFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.USER_ID.eq(userId))
                .fetch();
    }

    public List<UsersSessionsRecord> deleteOverdueSessions() {
        return context.deleteFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.EXPIRES_AT.lessThan(LocalDateTime.now()))
                .returning()
                .fetch();
    }
}

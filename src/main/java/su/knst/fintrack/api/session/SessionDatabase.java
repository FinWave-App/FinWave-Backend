package su.knst.fintrack.api.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;

import java.time.LocalDateTime;
import java.util.List;

import static su.knst.fintrack.jooq.Tables.TRANSACTIONS;
import static su.knst.fintrack.jooq.Tables.USERS_SESSIONS;

@Singleton
public class SessionDatabase {
    protected DSLContext context;

    @Inject
    public SessionDatabase(Database database) {
        this.context = database.context();
    }

    public void newSession(int userId, String token, int lifetimeDays, String description) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(lifetimeDays);

        context.insertInto(USERS_SESSIONS)
                .set(USERS_SESSIONS.USER_ID, userId)
                .set(USERS_SESSIONS.TOKEN, token)
                .set(USERS_SESSIONS.CREATED_AT, now)
                .set(USERS_SESSIONS.EXPIRES_AT, expires)
                .set(USERS_SESSIONS.DESCRIPTION, description)
                .execute();
    }

    public void updateSessionLifetime(String token, int lifetimeDays) {
        context.update(USERS_SESSIONS)
                .set(USERS_SESSIONS.EXPIRES_AT, LocalDateTime.now().plusDays(lifetimeDays))
                .where(USERS_SESSIONS.TOKEN.eq(token))
                .execute();
    }

    public void deleteSession(String token) {
        context.deleteFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.TOKEN.eq(token))
                .execute();
    }

    public void deleteSession(long sessionId) {
        context.deleteFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.ID.eq(sessionId))
                .execute();
    }

    public void deleteAllUserSessions(int userId) {
        context.deleteFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.USER_ID.eq(userId))
                .execute();
    }

    public boolean userOwnToken(int userId, String token) {
        return context.select(USERS_SESSIONS.ID)
                .from(USERS_SESSIONS)
                .where(USERS_SESSIONS.USER_ID.eq(userId).and(USERS_SESSIONS.TOKEN.eq(token)))
                .fetchOptional()
                .isPresent();
    }

    public boolean userOwnSession(int userId, long sessionId) {
        return context.select(USERS_SESSIONS.ID)
                .from(USERS_SESSIONS)
                .where(USERS_SESSIONS.USER_ID.eq(userId).and(USERS_SESSIONS.ID.eq(sessionId)))
                .fetchOptional()
                .isPresent();
    }

    public List<UsersSessionsRecord> getUserSessions(int userId) {
        return context.selectFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.USER_ID.eq(userId))
                .fetch();
    }
}

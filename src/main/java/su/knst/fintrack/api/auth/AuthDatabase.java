package su.knst.fintrack.api.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.jooq.tables.records.UsersRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.PBKDF2;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Optional;

import static su.knst.fintrack.jooq.Tables.USERS;
import static su.knst.fintrack.jooq.Tables.USERS_SESSIONS;

@Singleton
public class AuthDatabase {
    protected DSLContext context;

    @Inject
    public AuthDatabase(Database database) {
        this.context = database.context();
    }

    public void newSession(int userId, String token, int lifetimeDays, String description) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(lifetimeDays);

        InsertSetMoreStep<UsersSessionsRecord> step =
                context.insertInto(USERS_SESSIONS)
                        .set(USERS_SESSIONS.USER_ID, userId)
                        .set(USERS_SESSIONS.TOKEN, token)
                        .set(USERS_SESSIONS.CREATED_AT, now)
                        .set(USERS_SESSIONS.EXPIRES_AT, expires);

        if (description != null)
            step = step.set(USERS_SESSIONS.DESCRIPTION, description);

        step.execute();
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

    public Optional<UsersRecord> authUser(String login, String password) {
        Optional<UsersRecord> record = context.selectFrom(USERS)
                .where(USERS.USERNAME.eq(login))
                .fetchOptional();

        try {
            if (record.isEmpty() || !PBKDF2.verifyBase64(password, record.get().getPassword()))
                return Optional.empty();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        return record;
    }

    public Optional<UsersSessionsRecord> authUser(String sessionToken) {
        return context.selectFrom(USERS_SESSIONS)
                .where(USERS_SESSIONS.TOKEN.eq(sessionToken))
                .fetchOptional();
    }
}

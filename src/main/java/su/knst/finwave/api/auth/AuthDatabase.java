package su.knst.finwave.api.auth;

import org.jooq.DSLContext;
import su.knst.finwave.database.AbstractDatabase;
import su.knst.finwave.jooq.tables.records.UsersRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.PBKDF2;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.USERS;
import static su.knst.finwave.jooq.Tables.USERS_SESSIONS;

public class AuthDatabase extends AbstractDatabase{

    public AuthDatabase(DSLContext context) {
        super(context);
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

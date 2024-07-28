package app.finwave.backend.api.auth;

import org.jooq.DSLContext;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.UsersRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.PBKDF2;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.USERS;
import static app.finwave.backend.jooq.Tables.USERS_SESSIONS;

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
}

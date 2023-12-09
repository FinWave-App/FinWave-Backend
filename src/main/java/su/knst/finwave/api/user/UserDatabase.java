package su.knst.finwave.api.user;

import org.jooq.DSLContext;
import org.jooq.Record1;
import su.knst.finwave.database.AbstractDatabase;
import su.knst.finwave.utils.PBKDF2;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.USERS;

public class UserDatabase extends AbstractDatabase {

    public UserDatabase(DSLContext context) {
        super(context);
    }

    public boolean userExists(String username) {
        return context.select(USERS.ID)
                .from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOptional()
                .isPresent();
    }

    public Optional<Integer> registerUser(String username, String password) {
        String hashedPassword;

        try {
            hashedPassword = PBKDF2.hashWithSaltBase64(password);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        return context.insertInto(USERS)
                .set(USERS.USERNAME, username)
                .set(USERS.PASSWORD, hashedPassword)
                .returningResult(USERS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public void changeUserPassword(int userId, String password) {
        String hashedPassword;

        try {
            hashedPassword = PBKDF2.hashWithSaltBase64(password);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        context.update(USERS)
                .set(USERS.PASSWORD, hashedPassword)
                .where(USERS.ID.eq(userId))
                .execute();
    }

    public String getUsername(int userId) {
        return context.select(USERS.USERNAME)
                .from(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(null);
    }
}

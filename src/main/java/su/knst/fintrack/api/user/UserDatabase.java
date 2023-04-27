package su.knst.fintrack.api.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record1;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.utils.PBKDF2;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import static su.knst.fintrack.jooq.Tables.USERS;

@Singleton
public class UserDatabase {
    protected DSLContext context;

    @Inject
    public UserDatabase(Database database) {
        this.context = database.context();
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
}

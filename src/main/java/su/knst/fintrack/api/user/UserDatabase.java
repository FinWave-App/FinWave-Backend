package su.knst.fintrack.api.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import su.knst.fintrack.database.Database;

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
}

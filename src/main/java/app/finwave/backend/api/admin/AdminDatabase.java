package app.finwave.backend.api.admin;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.UsersRecord;

import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static app.finwave.backend.jooq.Tables.*;

public class AdminDatabase extends AbstractDatabase {
    public AdminDatabase(DSLContext context) {
        super(context);
    }

    public List<UsersRecord> getUserList(int offset, int count, String nameFilter) {
        return context.selectFrom(USERS)
                .where(nameFilter == null ? DSL.trueCondition() : USERS.USERNAME.containsIgnoreCase(nameFilter))
                .orderBy(USERS.ID)
                .limit(offset, count)
                .fetch();
    }

    public int getActiveUsersCount() {
        return context.select(countDistinct(USERS_SESSIONS.USER_ID))
                .from(USERS_SESSIONS)
                .where(USERS_SESSIONS.EXPIRES_AT
                        .greaterThan(LocalDateTime.now()))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public int getUsersCount() {
        return context.fetchCount(USERS);
    }

    public int getTransactionsCount() {
        return context.fetchCount(TRANSACTIONS);
    }
}

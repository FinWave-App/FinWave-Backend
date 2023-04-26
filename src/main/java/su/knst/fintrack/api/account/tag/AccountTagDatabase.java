package su.knst.fintrack.api.account.tag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.*;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.jooq.tables.AccountsTags;
import su.knst.fintrack.jooq.tables.records.AccountsTagsRecord;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static su.knst.fintrack.jooq.Tables.ACCOUNTS_TAGS;
import static su.knst.fintrack.jooq.Tables.NOTES;

@Singleton
public class AccountTagDatabase {
    protected DSLContext context;

    @Inject
    public AccountTagDatabase(Database database) {
        this.context = database.context();
    }

    public Optional<Long> newTag(int userId, String name, String description) {
        InsertSetMoreStep<AccountsTagsRecord> insert = context.insertInto(ACCOUNTS_TAGS)
                .set(ACCOUNTS_TAGS.OWNER_ID, userId)
                .set(ACCOUNTS_TAGS.NAME, name);

        if (description != null)
            insert = insert.set(ACCOUNTS_TAGS.DESCRIPTION, description);

        return insert.returningResult(ACCOUNTS_TAGS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public Optional<Long> newTag(int userId, String name) {
        return newTag(userId, name, null);
    }

    public Optional<AccountsTagsRecord> getTag(long id) {
        return context.selectFrom(ACCOUNTS_TAGS)
                .where(ACCOUNTS_TAGS.ID.eq(id))
                .fetchOptional();
    }

    public List<AccountsTagsRecord> getTags(int userId) {
        return context.selectFrom(ACCOUNTS_TAGS)
                .where(ACCOUNTS_TAGS.OWNER_ID.eq(userId))
                .fetch();
    }

    public void editTagName(long id, String name) {
        context.update(ACCOUNTS_TAGS)
                .set(ACCOUNTS_TAGS.NAME, name)
                .where(ACCOUNTS_TAGS.ID.eq(id))
                .execute();
    }

    public void editTagDescription(long id, String description) {
        context.update(ACCOUNTS_TAGS)
                .set(ACCOUNTS_TAGS.DESCRIPTION, description)
                .where(ACCOUNTS_TAGS.ID.eq(id))
                .execute();
    }

    public boolean userOwnTag(int userId, long tagId) {
        return context.select(ACCOUNTS_TAGS.ID)
                .from(ACCOUNTS_TAGS)
                .where(ACCOUNTS_TAGS.OWNER_ID.eq(userId).and(ACCOUNTS_TAGS.ID.eq(tagId)))
                .fetchOptional()
                .isPresent();
    }

    public int getTagsCount(int userId) {
        return context.selectCount()
                .from(ACCOUNTS_TAGS)
                .where(ACCOUNTS_TAGS.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }
}

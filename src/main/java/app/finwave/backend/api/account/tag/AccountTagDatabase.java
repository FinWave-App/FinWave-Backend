package app.finwave.backend.api.account.tag;

import org.jooq.*;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.AccountsTagsRecord;

import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class AccountTagDatabase extends AbstractDatabase {

    public AccountTagDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newTag(int userId, String name, String description) {
        return context.insertInto(ACCOUNTS_TAGS)
                .set(ACCOUNTS_TAGS.OWNER_ID, userId)
                .set(ACCOUNTS_TAGS.NAME, name)
                .set(ACCOUNTS_TAGS.DESCRIPTION, description)
                .returningResult(ACCOUNTS_TAGS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public Optional<AccountsTagsRecord> getTag(long id) {
        return context.selectFrom(ACCOUNTS_TAGS)
                .where(ACCOUNTS_TAGS.ID.eq(id))
                .fetchOptional();
    }

    public List<AccountsTagsRecord> getTags(int userId) {
        return context.selectFrom(ACCOUNTS_TAGS)
                .where(ACCOUNTS_TAGS.OWNER_ID.eq(userId))
                .orderBy(ACCOUNTS_TAGS.ID)
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

    public boolean tagSafeToDelete(long tagId) {
        return context.selectCount()
                .from(ACCOUNTS)
                .where(ACCOUNTS.TAG_ID.eq(tagId))
                .fetchOptional()
                .map(Record1::component1)
                .map(c -> c == 0)
                .orElse(false);
    }

    public void deleteTag(long tagId) {
        context.deleteFrom(ACCOUNTS_TAGS)
                .where(ACCOUNTS_TAGS.ID.eq(tagId))
                .execute();
    }
}

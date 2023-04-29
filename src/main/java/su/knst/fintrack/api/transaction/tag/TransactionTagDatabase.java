package su.knst.fintrack.api.transaction.tag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Converters;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.jooq.tables.records.AccountsTagsRecord;
import su.knst.fintrack.jooq.tables.records.TransactionsTagsRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static su.knst.fintrack.jooq.Tables.ACCOUNTS_TAGS;
import static su.knst.fintrack.jooq.Tables.TRANSACTIONS_TAGS;

@Singleton
public class TransactionTagDatabase {

    protected DSLContext context;

    @Inject
    public TransactionTagDatabase(Database database) {
        this.context = database.context();
    }

    public Optional<Long> newTag(int userId, short type, BigDecimal expectedAmount, Long parentId, String name, String description) {
        return context.insertInto(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.OWNER_ID, userId)
                .set(TRANSACTIONS_TAGS.TYPE, type)
                .set(TRANSACTIONS_TAGS.EXPECTED_AMOUNT, expectedAmount)
                .set(TRANSACTIONS_TAGS.PARENT_ID, parentId)
                .set(TRANSACTIONS_TAGS.NAME, name)
                .set(TRANSACTIONS_TAGS.DESCRIPTION, description)
                .returningResult(TRANSACTIONS_TAGS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public List<TransactionsTagsRecord> getTags(int userId) {
        return context.selectFrom(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.OWNER_ID.eq(userId))
                .fetch();
    }

    public boolean userOwnTag(int userId, long tagId) {
        return context.select(TRANSACTIONS_TAGS.ID)
                .from(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.OWNER_ID.eq(userId).and(TRANSACTIONS_TAGS.ID.eq(tagId)))
                .fetchOptional()
                .isPresent();
    }

    public int getTagsCount(int userId) {
        return context.selectCount()
                .from(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public void editTagType(long tagId, short type) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.TYPE, type)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public void editTagExpectedAmount(long tagId, BigDecimal amount) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.EXPECTED_AMOUNT, amount)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public void editTagParentId(long tagId, long parentId) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.PARENT_ID, parentId)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public void editTagName(long tagId, String name) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.NAME, name)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public void editTagDescription(long tagId, String description) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.DESCRIPTION, description)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }
}

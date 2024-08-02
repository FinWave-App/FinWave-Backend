package app.finwave.backend.api.transaction.tag.managment;

import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.TransactionsTagsManagementRecord;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;
import static app.finwave.backend.jooq.Tables.TRANSACTIONS_TAGS_MANAGEMENT;

public class TagManagementDatabase extends AbstractDatabase {
    public TagManagementDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> add(int userId, long tagId, long currencyId, short dateType, BigDecimal amount) {
        return context.insertInto(TRANSACTIONS_TAGS_MANAGEMENT)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.OWNER_ID, userId)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.TAG_ID, tagId)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.CURRENCY_ID, currencyId)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.DATE_TYPE, dateType)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.AMOUNT, amount)
                .returningResult(TRANSACTIONS_TAGS_MANAGEMENT.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public void update(long managementId, long tagId, long currencyId, short dateType, BigDecimal amount) {
        context.update(TRANSACTIONS_TAGS_MANAGEMENT)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.TAG_ID, tagId)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.CURRENCY_ID, currencyId)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.DATE_TYPE, dateType)
                .set(TRANSACTIONS_TAGS_MANAGEMENT.AMOUNT, amount)
                .where(TRANSACTIONS_TAGS_MANAGEMENT.ID.eq(managementId))
                .returning()
                .fetch();
    }

    public Optional<TransactionsTagsManagementRecord> remove(long managementId) {
        return context.deleteFrom(TRANSACTIONS_TAGS_MANAGEMENT)
                .where(TRANSACTIONS_TAGS_MANAGEMENT.ID.eq(managementId))
                .returning()
                .fetchOptional();
    }

    public boolean managementExists(int userId, long tagId, long currencyId, long excludeManagement) {
        return context.select(TRANSACTIONS_TAGS_MANAGEMENT.ID)
                .from(TRANSACTIONS_TAGS_MANAGEMENT)
                .where(TRANSACTIONS_TAGS_MANAGEMENT.TAG_ID.eq(tagId)
                        .and(TRANSACTIONS_TAGS_MANAGEMENT.CURRENCY_ID.eq(currencyId))
                        .and(TRANSACTIONS_TAGS_MANAGEMENT.OWNER_ID.eq(userId)
                        .and(TRANSACTIONS_TAGS_MANAGEMENT.ID.notEqual(excludeManagement)))
                )
                .fetchOptional()
                .isPresent();
    }

    public List<TransactionsTagsManagementRecord> getList(int userId) {
        return context.selectFrom(TRANSACTIONS_TAGS_MANAGEMENT)
                .where(TRANSACTIONS_TAGS_MANAGEMENT.OWNER_ID.eq(userId))
                .fetch();
    }

    public boolean userOwnManagement(int userId, long managementId) {
        return context.select(TRANSACTIONS_TAGS_MANAGEMENT.ID)
                .from(TRANSACTIONS_TAGS_MANAGEMENT)
                .where(TRANSACTIONS_TAGS_MANAGEMENT.OWNER_ID.eq(userId).and(TRANSACTIONS_TAGS_MANAGEMENT.ID.eq(managementId)))
                .fetchOptional()
                .isPresent();
    }
}

package app.finwave.backend.api.transaction;

import org.jooq.*;
import org.jooq.Record;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.database.AbstractDatabase;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class TransactionDatabase extends AbstractDatabase {

    public TransactionDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> applyTransaction(int userId, long categoryId, long accountId, long currencyId, OffsetDateTime created, BigDecimal delta, String description) {
        return context.insertInto(TRANSACTIONS)
                .set(TRANSACTIONS.OWNER_ID, userId)
                .set(TRANSACTIONS.CATEGORY_ID, categoryId)
                .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                .set(TRANSACTIONS.CURRENCY_ID, currencyId)
                .set(TRANSACTIONS.CREATED_AT, created)
                .set(TRANSACTIONS.DELTA, delta)
                .set(TRANSACTIONS.DESCRIPTION, description)
                .returningResult(TRANSACTIONS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public int getTransactionsCount(int userId, TransactionsFilter filter) {
        Condition condition = generateFilterCondition(userId, filter);

        return context.selectCount()
                .from(TRANSACTIONS)
                .where(condition)
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public Optional<Record> getTransaction(long id) {
        return context.selectFrom(TRANSACTIONS
                        .leftJoin(TRANSACTIONS_METADATA)
                        .on(TRANSACTIONS.METADATA_ID.eq(TRANSACTIONS_METADATA.ID)))
                .where(TRANSACTIONS.ID.eq(id))
                .fetchOptional();
    }

    public List<Record> getTransactions(int userId, int offset, int count, TransactionsFilter filter) {
        Condition condition = generateFilterCondition(userId, filter);

        return context.selectFrom(TRANSACTIONS
                        .leftJoin(TRANSACTIONS_METADATA)
                        .on(TRANSACTIONS.METADATA_ID.eq(TRANSACTIONS_METADATA.ID)))
                .where(condition)
                .orderBy(TRANSACTIONS.CREATED_AT.desc(), TRANSACTIONS.ID.desc())
                .limit(offset, count)
                .fetch();
    }

    public static Condition generateFilterCondition(int userId, TransactionsFilter filter) {
        Condition condition = TRANSACTIONS.OWNER_ID.eq(userId);

        if (filter.getCategoriesIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.CATEGORY_ID, filter.getCategoriesIds()));

        if (filter.getAccountIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.ACCOUNT_ID, filter.getAccountIds()));

        if (filter.getCurrenciesIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.CURRENCY_ID, filter.getCurrenciesIds()));

        if (filter.getFromTime() != null)
            condition = condition.and(TRANSACTIONS.CREATED_AT.greaterOrEqual(filter.getFromTime()));

        if (filter.getToTime() != null)
            condition = condition.and(TRANSACTIONS.CREATED_AT.lessOrEqual(filter.getToTime()));

        if (filter.getDescription() != null)
            condition = condition.and(TRANSACTIONS.DESCRIPTION.containsIgnoreCase(filter.getDescription()));

        return condition;
    }

    protected static <T> Condition generateFilterAnyCondition(TableField<?, T> field, List<T> values) {
        Condition condition = null;

        for (T value : values) {
            if (condition == null) {
                condition = field.eq(value);

                continue;
            }

            condition = condition.or(field.eq(value));
        }

        return condition;
    }

    public void deleteTransaction(long transactionId) {
        context.deleteFrom(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(transactionId))
                .execute();
    }

    public void editTransaction(long transactionId, long categoryId, long accountId, long currencyId, OffsetDateTime created, BigDecimal delta, String description) {
        var update = context.update(TRANSACTIONS)
                .set(TRANSACTIONS.CATEGORY_ID, categoryId)
                .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                .set(TRANSACTIONS.CURRENCY_ID, currencyId);

        if (created != null)
            update = update.set(TRANSACTIONS.CREATED_AT, created);

        update.set(TRANSACTIONS.DELTA, delta)
                .set(TRANSACTIONS.DESCRIPTION, description)
                .where(TRANSACTIONS.ID.eq(transactionId))
                .execute();
    }

    public boolean userOwnTransaction(int userId, long transactionId) {
        return context.select(TRANSACTIONS.ID)
                .from(TRANSACTIONS)
                .where(TRANSACTIONS.OWNER_ID.eq(userId).and(TRANSACTIONS.ID.eq(transactionId)))
                .fetchOptional()
                .isPresent();
    }
}

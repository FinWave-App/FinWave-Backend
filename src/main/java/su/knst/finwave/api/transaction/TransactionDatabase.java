package su.knst.finwave.api.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.*;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.database.Database;
import su.knst.finwave.jooq.tables.records.TransactionsRecord;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.*;

@Singleton
public class TransactionDatabase {

    protected DSLContext context;

    @Inject
    public TransactionDatabase(Database database) {
        this.context = database.context();
    }

    public long applyTransaction(int userId, long tagId, long accountId, OffsetDateTime created, BigDecimal delta, String description) {
        return context.transactionResult((configuration) -> {
            DSLContext transaction = configuration.dsl();

            Field<Long> currencyId = transaction.select(ACCOUNTS.CURRENCY_ID)
                    .from(ACCOUNTS)
                    .where(ACCOUNTS.ID.eq(accountId))
                    .asField();

            Optional<Long> record = transaction.insertInto(TRANSACTIONS)
                    .set(TRANSACTIONS.OWNER_ID, userId)
                    .set(TRANSACTIONS.TAG_ID, tagId)
                    .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                    .set(TRANSACTIONS.CURRENCY_ID, currencyId)
                    .set(TRANSACTIONS.CREATED_AT, created)
                    .set(TRANSACTIONS.DELTA, delta)
                    .set(TRANSACTIONS.DESCRIPTION, description)
                    .returningResult(TRANSACTIONS.ID)
                    .fetchOptional()
                    .map(Record1::component1);

            if (record.isEmpty())
                throw new RuntimeException("Fail to create new transaction");

            transaction.update(ACCOUNTS)
                    .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
                    .where(ACCOUNTS.ID.eq(accountId))
                    .execute();

            return record.get();
        });
    }

    public void cancelTransaction(long transactionId) {
        context.transaction((configuration) -> {
            DSLContext transaction = configuration.dsl();

            Optional<TransactionsRecord> record = transaction.selectFrom(TRANSACTIONS)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .fetchOptional();

            if (record.isEmpty())
                throw new RuntimeException("Transaction not exists");

            transaction.deleteFrom(TRANSACTIONS)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .execute();

            transaction.update(ACCOUNTS)
                    .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.get().getDelta()))
                    .where(ACCOUNTS.ID.eq(record.get().getAccountId()))
                    .execute();
        });
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

    public List<TransactionsRecord> getTransactions(int userId, int offset, int count, TransactionsFilter filter) {
        Condition condition = generateFilterCondition(userId, filter);

        return context.selectFrom(TRANSACTIONS)
                .where(condition)
                .orderBy(TRANSACTIONS.CREATED_AT.desc(), TRANSACTIONS.ID.desc())
                .limit(offset, count)
                .fetch();
    }

    public Condition generateFilterCondition(int userId, TransactionsFilter filter) {
        Condition condition = TRANSACTIONS.OWNER_ID.eq(userId);

        if (filter.getTagsIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.TAG_ID, filter.getTagsIds()));

        if (filter.getAccountIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.ACCOUNT_ID, filter.getAccountIds()));

        if (filter.getCurrenciesIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.CURRENCY_ID, filter.getCurrenciesIds()));

        if (filter.getFromTime() != null)
            condition = condition.and(TRANSACTIONS.CREATED_AT.greaterOrEqual(filter.getFromTime()));

        if (filter.getToTime() != null)
            condition = condition.and(TRANSACTIONS.CREATED_AT.lessOrEqual(filter.getToTime()));

        if (filter.getDescriptionContains() != null)
            condition = condition.and(TRANSACTIONS.DESCRIPTION.containsIgnoreCase(filter.getDescriptionContains()));

        return condition;
    }

    protected <T> Condition generateFilterAnyCondition(TableField<?, T> field, List<T> values) {
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

    public void editTransaction(long transactionId, long tagId, long accountId, OffsetDateTime created, BigDecimal delta, String description) {
        context.transaction((configuration) -> {
            DSLContext transaction = configuration.dsl();

            Optional<TransactionsRecord> record = transaction.selectFrom(TRANSACTIONS)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .fetchOptional();

            if (record.isEmpty())
                throw new RuntimeException("Transaction not exists");

            transaction.update(ACCOUNTS)
                    .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.get().getDelta()))
                    .where(ACCOUNTS.ID.eq(record.get().getAccountId()))
                    .execute();

            Optional<Long> currencyId = transaction
                    .update(ACCOUNTS)
                    .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
                    .where(ACCOUNTS.ID.eq(accountId))
                    .returningResult(ACCOUNTS.CURRENCY_ID)
                    .fetchOptional()
                    .map(Record1::component1);

            if (currencyId.isEmpty())
                throw new RuntimeException("Failed to get currency id and modify account");

            transaction.update(TRANSACTIONS)
                    .set(TRANSACTIONS.TAG_ID, tagId)
                    .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                    .set(TRANSACTIONS.CURRENCY_ID, currencyId.get())
                    .set(TRANSACTIONS.CREATED_AT, created)
                    .set(TRANSACTIONS.DELTA, delta)
                    .set(TRANSACTIONS.DESCRIPTION, description)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .execute();
        });
    }

    public boolean userOwnTransaction(int userId, long transactionId) {
        return context.select(TRANSACTIONS.ID)
                .from(TRANSACTIONS)
                .where(TRANSACTIONS.OWNER_ID.eq(userId).and(TRANSACTIONS.ID.eq(transactionId)))
                .fetchOptional()
                .isPresent();
    }
}

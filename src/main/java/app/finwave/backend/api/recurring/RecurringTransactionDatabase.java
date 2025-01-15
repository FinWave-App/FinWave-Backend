package app.finwave.backend.api.recurring;

import org.jooq.DSLContext;
import org.jooq.Record1;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.RecurringTransactionsRecord;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;


public class RecurringTransactionDatabase extends AbstractDatabase {

    public RecurringTransactionDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newRecurring(int userId, long categoryId, long accountId,
                                       RepeatType repeatType, short repeatArg, NotificationMode notificationMode,
                                       OffsetDateTime nextRepeat, BigDecimal delta, String description) {

        Long currencyId = context.select(ACCOUNTS.CURRENCY_ID)
                .from(ACCOUNTS)
                .where(ACCOUNTS.ID.eq(accountId))
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        return context.insertInto(RECURRING_TRANSACTIONS)
                .set(RECURRING_TRANSACTIONS.OWNER_ID, userId)
                .set(RECURRING_TRANSACTIONS.CATEGORY_ID, categoryId)
                .set(RECURRING_TRANSACTIONS.ACCOUNT_ID, accountId)
                .set(RECURRING_TRANSACTIONS.CURRENCY_ID, currencyId)
                .set(RECURRING_TRANSACTIONS.REPEAT_FUNC, (short)repeatType.ordinal())
                .set(RECURRING_TRANSACTIONS.REPEAT_FUNC_ARG, repeatArg)
                .set(RECURRING_TRANSACTIONS.NOTIFICATION_MODE, (short)notificationMode.ordinal())
                .set(RECURRING_TRANSACTIONS.LAST_REPEAT, OffsetDateTime.now())
                .set(RECURRING_TRANSACTIONS.NEXT_REPEAT, nextRepeat)
                .set(RECURRING_TRANSACTIONS.DELTA, delta)
                .set(RECURRING_TRANSACTIONS.DESCRIPTION, description)
                .returningResult(RECURRING_TRANSACTIONS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public void editRecurring(long id, long categoryId, long accountId,
                                       RepeatType repeatType, short repeatArg, NotificationMode notificationMode,
                                       OffsetDateTime nextRepeat, BigDecimal delta, String description) {

        Long currencyId = context.select(ACCOUNTS.CURRENCY_ID)
                .from(ACCOUNTS)
                .where(ACCOUNTS.ID.eq(accountId))
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        context.update(RECURRING_TRANSACTIONS)
                .set(RECURRING_TRANSACTIONS.CATEGORY_ID, categoryId)
                .set(RECURRING_TRANSACTIONS.ACCOUNT_ID, accountId)
                .set(RECURRING_TRANSACTIONS.CURRENCY_ID, currencyId)
                .set(RECURRING_TRANSACTIONS.REPEAT_FUNC, (short)repeatType.ordinal())
                .set(RECURRING_TRANSACTIONS.REPEAT_FUNC_ARG, repeatArg)
                .set(RECURRING_TRANSACTIONS.NOTIFICATION_MODE, (short)notificationMode.ordinal())
                .set(RECURRING_TRANSACTIONS.NEXT_REPEAT, nextRepeat)
                .set(RECURRING_TRANSACTIONS.DELTA, delta)
                .set(RECURRING_TRANSACTIONS.DESCRIPTION, description)
                .where(RECURRING_TRANSACTIONS.ID.eq(id))
                .execute();
    }

    public void deleteRecurring(long id) {
        context.deleteFrom(RECURRING_TRANSACTIONS)
                .where(RECURRING_TRANSACTIONS.ID.eq(id))
                .execute();
    }

    public List<RecurringTransactionsRecord> getList(int userId) {
        return context.selectFrom(RECURRING_TRANSACTIONS)
                .where(RECURRING_TRANSACTIONS.OWNER_ID.eq(userId))
                .orderBy(RECURRING_TRANSACTIONS.NEXT_REPEAT.asc(), RECURRING_TRANSACTIONS.ID.desc())
                .fetch();
    }

    public boolean accountAffected(long accountId) {
        return context.selectCount()
                .from(RECURRING_TRANSACTIONS)
                .where(RECURRING_TRANSACTIONS.ACCOUNT_ID.eq(accountId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0) > 0;
    }

    public boolean userOwnRecurringTransaction(int userId, long id) {
        return context.select(RECURRING_TRANSACTIONS.ID)
                .from(RECURRING_TRANSACTIONS)
                .where(RECURRING_TRANSACTIONS.OWNER_ID.eq(userId).and(RECURRING_TRANSACTIONS.ID.eq(id)))
                .fetchOptional()
                .isPresent();
    }

    public List<RecurringTransactionsRecord> getRecurringForProcessing() {
        return context.selectFrom(RECURRING_TRANSACTIONS)
                .where(RECURRING_TRANSACTIONS.NEXT_REPEAT.lessOrEqual(OffsetDateTime.now()))
                .fetch();
    }

    public void updateRecurring(long id, OffsetDateTime lastRepeat, OffsetDateTime nextRepeat) {
        context.update(RECURRING_TRANSACTIONS)
                .set(RECURRING_TRANSACTIONS.LAST_REPEAT, lastRepeat)
                .set(RECURRING_TRANSACTIONS.NEXT_REPEAT, nextRepeat)
                .where(RECURRING_TRANSACTIONS.ID.eq(id))
                .execute();
    }
}

package su.knst.finwave.api.transaction.recurring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record1;
import su.knst.finwave.database.AbstractDatabase;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.RecurringTransactionsRecord;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.RECURRING_TRANSACTIONS;


public class RecurringTransactionDatabase extends AbstractDatabase {

    public RecurringTransactionDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newRecurring(int userId, long tagId, long accountId, long currencyId,
                                       RepeatType repeatType, short repeatArg, NotificationMode notificationMode,
                                       OffsetDateTime nextRepeat, BigDecimal delta, String description) {

        return context.insertInto(RECURRING_TRANSACTIONS)
                .set(RECURRING_TRANSACTIONS.OWNER_ID, userId)
                .set(RECURRING_TRANSACTIONS.TAG_ID, tagId)
                .set(RECURRING_TRANSACTIONS.ACCOUNT_ID, accountId)
                .set(RECURRING_TRANSACTIONS.CURRENCY_ID, currencyId)
                .set(RECURRING_TRANSACTIONS.REPEAT_FUNC, (short)repeatType.ordinal())
                .set(RECURRING_TRANSACTIONS.REPEAT_FUNC_ARG, repeatArg)
                .set(RECURRING_TRANSACTIONS.NOTIFICATION_MODE, (short)notificationMode.ordinal())
                .set(RECURRING_TRANSACTIONS.LAST_REPEAT, OffsetDateTime.MIN)
                .set(RECURRING_TRANSACTIONS.NEXT_REPEAT, nextRepeat)
                .set(RECURRING_TRANSACTIONS.DELTA, delta)
                .set(RECURRING_TRANSACTIONS.DESCRIPTION, description)
                .returningResult(RECURRING_TRANSACTIONS.ID)
                .fetchOptional()
                .map(Record1::component1);
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

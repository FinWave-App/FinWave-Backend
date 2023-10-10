package su.knst.finwave.api.analytics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Condition;
import org.jooq.DSLContext;
import su.knst.finwave.api.analytics.result.AnalyticsByDays;
import su.knst.finwave.api.analytics.result.AnalyticsByMonths;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.database.AbstractDatabase;
import su.knst.finwave.database.DatabaseWorker;

import static org.jooq.impl.DSL.*;
import static su.knst.finwave.jooq.Tables.TRANSACTIONS;

public class AnalyticsDatabase extends AbstractDatabase {

    public AnalyticsDatabase(DSLContext context) {
        super(context);
    }

    public AnalyticsByMonths getAnalyticsByMonths(int userId, TransactionsFilter filter) {
        Condition condition = TransactionDatabase.generateFilterCondition(userId, filter);

        var result = context.select(TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.TAG_ID,
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT),
                        sum(TRANSACTIONS.DELTA))
                .from(TRANSACTIONS)
                .where(condition)
                .groupBy(TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.TAG_ID,
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT))
                .fetch();

        return new AnalyticsByMonths(result);
    }

    public AnalyticsByDays getAnalyticsByDays(int userId, TransactionsFilter filter) {
        Condition condition = TransactionDatabase.generateFilterCondition(userId, filter);

        var result = context.select(TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.TAG_ID,
                        day(TRANSACTIONS.CREATED_AT),
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT),
                        sum(TRANSACTIONS.DELTA))
                .from(TRANSACTIONS)
                .where(condition)
                .groupBy(
                        TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.TAG_ID,
                        day(TRANSACTIONS.CREATED_AT),
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT))
                .fetch();

        return new AnalyticsByDays(result);
    }
}

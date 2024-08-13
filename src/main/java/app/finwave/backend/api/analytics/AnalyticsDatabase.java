package app.finwave.backend.api.analytics;

import org.jooq.Condition;
import org.jooq.DSLContext;
import app.finwave.backend.api.analytics.result.AnalyticsByDays;
import app.finwave.backend.api.analytics.result.AnalyticsByMonths;
import app.finwave.backend.api.transaction.TransactionDatabase;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.database.AbstractDatabase;

import static org.jooq.impl.DSL.*;
import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

public class AnalyticsDatabase extends AbstractDatabase {

    public AnalyticsDatabase(DSLContext context) {
        super(context);
    }

    public AnalyticsByMonths getAnalyticsByMonths(int userId, TransactionsFilter filter) {
        Condition condition = TransactionDatabase.generateFilterCondition(userId, filter);

        var result = context.select(TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.CATEGORY_ID,
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT),
                        sum(TRANSACTIONS.DELTA))
                .from(TRANSACTIONS)
                .where(condition)
                .groupBy(TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.CATEGORY_ID,
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT))
                .fetch();

        return new AnalyticsByMonths(result);
    }

    public AnalyticsByDays getAnalyticsByDays(int userId, TransactionsFilter filter) {
        Condition condition = TransactionDatabase.generateFilterCondition(userId, filter);

        var result = context.select(TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.CATEGORY_ID,
                        day(TRANSACTIONS.CREATED_AT),
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT),
                        sum(TRANSACTIONS.DELTA))
                .from(TRANSACTIONS)
                .where(condition)
                .groupBy(
                        TRANSACTIONS.CURRENCY_ID,
                        TRANSACTIONS.CATEGORY_ID,
                        day(TRANSACTIONS.CREATED_AT),
                        month(TRANSACTIONS.CREATED_AT),
                        year(TRANSACTIONS.CREATED_AT))
                .fetch();

        return new AnalyticsByDays(result);
    }
}

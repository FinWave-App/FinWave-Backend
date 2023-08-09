package su.knst.fintrack.api.analytics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import su.knst.fintrack.api.analytics.result.AnalyticsByDays;
import su.knst.fintrack.api.analytics.result.AnalyticsByMonths;
import su.knst.fintrack.api.transaction.TransactionDatabase;
import su.knst.fintrack.api.transaction.filter.TransactionsFilter;
import su.knst.fintrack.database.Database;

import java.time.OffsetDateTime;

import static org.jooq.impl.DSL.*;
import static su.knst.fintrack.jooq.Tables.TRANSACTIONS;

@Singleton
public class AnalyticsDatabase {
    protected DSLContext context;
    protected TransactionDatabase transactionDatabase;

    @Inject
    public AnalyticsDatabase(Database database, TransactionDatabase transactionDatabase) {
        this.context = database.context();
        this.transactionDatabase = transactionDatabase;
    }

    public AnalyticsByMonths getAnalyticsByMonths(int userId, TransactionsFilter filter) {
        Condition condition = transactionDatabase.generateFilterCondition(userId, filter);

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
        Condition condition = transactionDatabase.generateFilterCondition(userId, filter);

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

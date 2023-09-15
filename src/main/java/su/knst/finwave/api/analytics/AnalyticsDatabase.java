package su.knst.finwave.api.analytics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Condition;
import org.jooq.DSLContext;
import su.knst.finwave.api.analytics.result.AnalyticsByDays;
import su.knst.finwave.api.analytics.result.AnalyticsByMonths;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.TransactionDatabaseProvider;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.database.Database;

import static org.jooq.impl.DSL.*;
import static su.knst.finwave.jooq.Tables.TRANSACTIONS;

@Singleton
public class AnalyticsDatabase {
    protected DSLContext context;
    protected TransactionDatabase transactionDatabase;

    @Inject
    public AnalyticsDatabase(Database database, TransactionDatabaseProvider provider) {
        this.context = database.context();
        this.transactionDatabase = provider.get();
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

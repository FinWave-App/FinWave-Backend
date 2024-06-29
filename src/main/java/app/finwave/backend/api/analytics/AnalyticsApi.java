package app.finwave.backend.api.analytics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.analytics.result.AnalyticsByDays;
import app.finwave.backend.api.analytics.result.AnalyticsByMonths;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.AnalyticsConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;

import java.time.OffsetDateTime;

@Singleton
public class AnalyticsApi {
    protected AnalyticsDatabase database;
    protected AnalyticsConfig config;

    @Inject
    public AnalyticsApi(Configs configs, DatabaseWorker databaseWorker) {
        this.config = configs.getState(new AnalyticsConfig());
        this.database = databaseWorker.get(AnalyticsDatabase.class);
    }

    public Object getAnalyticsByMonths(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        TransactionsFilter filter = new TransactionsFilter(request);

        if (filter.getFromTime() == null)
            filter.setFromTime(OffsetDateTime.now().minusDays(config.maxTimeRangeDaysForMonths / 2));

        if (filter.getToTime() == null)
            filter.setToTime(OffsetDateTime.now());

        if (!filter.validateTime(config.maxTimeRangeDaysForMonths))
            throw new InvalidParameterException("Invalid date");

        AnalyticsByMonths analytics = database.getAnalyticsByMonths(sessionsRecord.getUserId(), filter);

        response.status(200);

        return analytics;
    }

    public Object getAnalyticsByDays(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        TransactionsFilter filter = new TransactionsFilter(request);

        if (filter.getFromTime() == null)
            filter.setFromTime(OffsetDateTime.now().minusDays(config.maxTimeRangeDaysForDays / 2));

        if (filter.getToTime() == null)
            filter.setToTime(OffsetDateTime.now());

        if (!filter.validateTime(config.maxTimeRangeDaysForDays))
            throw new IllegalArgumentException();

        AnalyticsByDays analytics = database.getAnalyticsByDays(sessionsRecord.getUserId(), filter);

        response.status(200);

        return analytics;
    }
}

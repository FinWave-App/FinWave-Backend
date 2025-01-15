package app.finwave.backend.api.analytics;

import app.finwave.backend.api.analytics.result.*;
import app.finwave.backend.utils.params.ParamsValidator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.AnalyticsConfig;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;

import java.time.OffsetDateTime;
import java.util.List;

@Singleton
public class AnalyticsApi {
    protected AnalyticsManager manager;
    protected AnalyticsConfig config;

    @Inject
    public AnalyticsApi(Configs configs, AnalyticsManager manager) {
        this.config = configs.getState(new AnalyticsConfig());
        this.manager = manager;
    }

    public Object getCategoriesAnalytics(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        OffsetDateTime time = ParamsValidator
                .string(request, "time")
                .optional()
                .map(OffsetDateTime::parse)
                .orElseGet(OffsetDateTime::now);

        List<CategorySummaryWithBudget> summaryList = manager.getCategoriesAnalytics(sessionsRecord.getUserId(), time);

        response.status(200);

        return new CategoriesAnalytics(summaryList);
    }

    public Object getAnalyticsByMonths(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        TransactionsFilter filter = new TransactionsFilter(request);

        if (filter.getFromTime() == null)
            filter = filter.setFromTime(OffsetDateTime.now().minusDays(config.maxTimeRangeDaysForMonths / 2));

        if (filter.getToTime() == null)
            filter = filter.setToTime(OffsetDateTime.now());

        if (!filter.validateTime(config.maxTimeRangeDaysForMonths))
            throw new InvalidParameterException();

        AnalyticsByMonths analytics = manager.getAnalyticsByMonths(sessionsRecord.getUserId(), filter);

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

        AnalyticsByDays analytics = manager.getAnalyticsByDays(sessionsRecord.getUserId(), filter);

        response.status(200);

        return analytics;
    }
}

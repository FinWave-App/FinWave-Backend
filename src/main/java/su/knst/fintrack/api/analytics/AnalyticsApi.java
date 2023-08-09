package su.knst.fintrack.api.analytics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.analytics.result.AnalyticsByDays;
import su.knst.fintrack.api.analytics.result.AnalyticsByMonths;
import su.knst.fintrack.api.note.NoteApi;
import su.knst.fintrack.api.note.NoteDatabase;
import su.knst.fintrack.api.transaction.filter.TransactionsFilter;
import su.knst.fintrack.api.user.UserSettingsDatabase;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.app.AnalyticsConfig;
import su.knst.fintrack.config.app.NotesConfig;
import su.knst.fintrack.jooq.tables.records.NotesRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AnalyticsApi {
    protected AnalyticsDatabase database;
    protected AnalyticsConfig config;

    @Inject
    public AnalyticsApi(Configs configs, AnalyticsDatabase database) {
        this.config = configs.getState(new AnalyticsConfig());
        this.database = database;
    }

    public Object getAnalyticsByMonths(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        TransactionsFilter filter = new TransactionsFilter(request);

        if (filter.getFromTime() == null)
            filter.setFromTime(OffsetDateTime.now().minusDays(config.maxTimeRangeDaysForMonths / 2));

        if (filter.getToTime() == null)
            filter.setToTime(OffsetDateTime.now());

        if (!filter.validateTime(config.maxTimeRangeDaysForMonths))
            throw new IllegalArgumentException();

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

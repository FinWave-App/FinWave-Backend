package su.knst.finwave.service.reports;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import su.knst.finwave.api.notification.manager.NotificationManager;
import su.knst.finwave.api.report.ReportDatabase;
import su.knst.finwave.api.transaction.manager.TransactionsManager;
import su.knst.finwave.api.transaction.recurring.RecurringTransactionDatabase;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.ReportsRecord;
import su.knst.finwave.report.ReportFileWorker;
import su.knst.finwave.service.AbstractService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class ReportsService extends AbstractService {
    protected ReportDatabase database;

    @Inject
    public ReportsService(DatabaseWorker databaseWorker) {
        this.database = databaseWorker.get(ReportDatabase.class);
    }

    @Override
    public void run() {
        List<ReportsRecord> reportsToRemove = database.getToRemove(50);

        for (ReportsRecord record : reportsToRemove) {
            try {
                ReportFileWorker.delete(record.getId());
                database.removeReport(record.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public long getRepeatTime() {
        return 1;
    }

    @Override
    public long getInitDelay() {
        return 0;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.MINUTES;
    }

    @Override
    public String name() {
        return "Reports Garbage";
    }
}

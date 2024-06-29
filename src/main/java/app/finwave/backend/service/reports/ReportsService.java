package app.finwave.backend.service.reports;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import app.finwave.backend.api.notification.manager.NotificationManager;
import app.finwave.backend.api.report.ReportDatabase;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.transaction.recurring.RecurringTransactionDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.report.ReportFileWorker;
import app.finwave.backend.service.AbstractService;

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

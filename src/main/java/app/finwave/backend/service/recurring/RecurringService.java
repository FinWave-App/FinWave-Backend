package app.finwave.backend.service.recurring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import app.finwave.backend.api.notification.data.Notification;
import app.finwave.backend.api.notification.data.NotificationOptions;
import app.finwave.backend.api.notification.manager.NotificationManager;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.api.recurring.NotificationMode;
import app.finwave.backend.api.recurring.RecurringTransactionDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.RecurringTransactionsRecord;
import app.finwave.backend.service.AbstractService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class RecurringService extends AbstractService {

    protected RecurringTransactionDatabase database;
    protected NotificationManager notificationManager;
    protected TransactionsManager transactionsManager;

    @Inject
    public RecurringService(DatabaseWorker databaseWorker, TransactionsManager transactionsManager, NotificationManager notificationManager) {
        this.database = databaseWorker.get(RecurringTransactionDatabase.class);
        this.notificationManager = notificationManager;
        this.transactionsManager = transactionsManager;
    }

    @Override
    public void run() {
        List<RecurringTransactionsRecord> records = database.getRecurringForProcessing();

        for (RecurringTransactionsRecord record : records) {
            transactionsManager.applyRecurringTransaction(new TransactionNewRecord(
                    record.getOwnerId(),
                    record.getCategoryId(),
                    record.getAccountId(),
                    record.getNextRepeat(),
                    record.getDelta(),
                    record.getDescription()
            ));

            database.updateRecurring(
                    record.getId(),
                    record.getNextRepeat(),
                    NextRepeatTools.calculate(record.getNextRepeat(), record.getRepeatFunc(), record.getRepeatFuncArg())
            );

            NotificationMode mode = NotificationMode.values()[(int)record.getNotificationMode()];
            String message = record.getDescription();

            if (message == null || message.isBlank())
                message = "Some recurring is completed";

            if (mode != NotificationMode.WITHOUT)
                notificationManager.push(Notification.create(
                        message,
                        new NotificationOptions(mode == NotificationMode.SILENT, -1, null),
                        record.getOwnerId()
                ));
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
        return "Recurring Transactions";
    }
}

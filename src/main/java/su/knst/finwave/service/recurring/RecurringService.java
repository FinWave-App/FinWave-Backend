package su.knst.finwave.service.recurring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import su.knst.finwave.api.notification.NotificationDatabase;
import su.knst.finwave.api.notification.data.NotificationOptions;
import su.knst.finwave.api.transaction.manager.TransactionsManager;
import su.knst.finwave.api.transaction.manager.records.TransactionNewRecord;
import su.knst.finwave.api.transaction.recurring.NotificationMode;
import su.knst.finwave.api.transaction.recurring.RecurringTransactionDatabase;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.RecurringTransactionsRecord;
import su.knst.finwave.service.AbstractService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class RecurringService extends AbstractService {

    protected RecurringTransactionDatabase database;
    protected NotificationDatabase notificationDatabase;
    protected TransactionsManager transactionsManager;

    @Inject
    public RecurringService(DatabaseWorker databaseWorker, TransactionsManager transactionsManager) {
        this.database = databaseWorker.get(RecurringTransactionDatabase.class);
        this.notificationDatabase = databaseWorker.get(NotificationDatabase.class);
        this.transactionsManager = transactionsManager;
    }

    @Override
    public void run() {
        List<RecurringTransactionsRecord> records = database.getRecurringForProcessing();

        for (RecurringTransactionsRecord record : records) {
            long newTransaction = transactionsManager.applyRecurringTransaction(new TransactionNewRecord(
                    record.getOwnerId(),
                    record.getTagId(),
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
                notificationDatabase.pushNotification(record.getOwnerId(), message, new NotificationOptions(mode == NotificationMode.SILENT, -1, null));
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

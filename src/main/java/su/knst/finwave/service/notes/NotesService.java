package su.knst.finwave.service.notes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import su.knst.finwave.api.note.NoteDatabase;
import su.knst.finwave.api.notification.data.Notification;
import su.knst.finwave.api.notification.data.NotificationOptions;
import su.knst.finwave.api.notification.manager.NotificationManager;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.NotesRecord;
import su.knst.finwave.service.AbstractService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class NotesService extends AbstractService {
    protected NoteDatabase database;
    protected NotificationManager notificationManager;

    @Inject
    public NotesService(DatabaseWorker databaseWorker, NotificationManager notificationManager) {
        this.database = databaseWorker.get(NoteDatabase.class);
        this.notificationManager = notificationManager;
    }

    @Override
    public void run() {
        while (true) {
            List<NotesRecord> toRemind = database.getToRemind(50);

            if (toRemind.isEmpty())
                break;

            for (NotesRecord note : toRemind) {
                notificationManager.push(Notification.create(
                        note.getNote(),
                        new NotificationOptions(false, -1, null),
                        note.getOwnerId()
                ));

                database.updateNotificationTime(note.getId(), null);
            }
        }
    }

    @Override
    public long getRepeatTime() {
        return 30;
    }

    @Override
    public long getInitDelay() {
        return 0;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public String name() {
        return "Notes";
    }
}

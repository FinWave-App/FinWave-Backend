package app.finwave.backend.service.notes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import app.finwave.backend.api.note.NoteDatabase;
import app.finwave.backend.api.notification.data.Notification;
import app.finwave.backend.api.notification.data.NotificationOptions;
import app.finwave.backend.api.notification.manager.NotificationManager;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.NotesRecord;
import app.finwave.backend.service.AbstractService;

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

package su.knst.fintrack.api.note;


import com.google.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record1;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.jooq.tables.records.NotesRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.fintrack.jooq.Tables.NOTES;

public class NoteDatabase {
    protected DSLContext context;

    @Inject
    public NoteDatabase(Database database) {
        this.context = database.context();
    }

    public Optional<Long> newNote(int userId, OffsetDateTime notificationTime, String note) {
        InsertSetMoreStep<NotesRecord> insert = context.insertInto(NOTES)
                .set(NOTES.OWNER_ID, userId)
                .set(NOTES.LAST_EDIT, OffsetDateTime.now())
                .set(NOTES.NOTE, note);

        if (notificationTime != null)
            insert = insert.set(NOTES.NOTIFICATION_TIME, notificationTime);

        return insert
                .returningResult(NOTES.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public Optional<NotesRecord> getNote(long id) {
        return context.selectFrom(NOTES)
                .where(NOTES.ID.eq(id))
                .fetchOptional();
    }

    public List<NotesRecord> getNotes(int userId) {
        return context.selectFrom(NOTES)
                .where(NOTES.OWNER_ID.eq(userId))
                .orderBy(NOTES.LAST_EDIT, NOTES.ID)
                .fetch();
    }

    public List<NotesRecord> getNotes(int userId, int offset, int count) {
        return context.selectFrom(NOTES)
                .where(NOTES.OWNER_ID.eq(userId))
                .orderBy(NOTES.LAST_EDIT, NOTES.ID)
                .limit(offset, count)
                .fetch();
    }

    public boolean userOwnNote(int userId, long noteId) {
        return context.select(NOTES.ID)
                .from(NOTES)
                .where(NOTES.OWNER_ID.eq(userId).and(NOTES.ID.eq(noteId)))
                .fetchOptional()
                .isPresent();
    }

    public List<NotesRecord> findNote(int userId, String containsText, int offset, int count) {
        return context.selectFrom(NOTES)
                .where(NOTES.OWNER_ID.eq(userId)
                        .and(NOTES.NOTE.contains(containsText))
                )
                .orderBy(NOTES.LAST_EDIT, NOTES.ID)
                .limit(offset, count)
                .fetch();
    }

    public void editNote(long id, String newText) {
        context.update(NOTES)
                .set(NOTES.LAST_EDIT, OffsetDateTime.now())
                .set(NOTES.NOTE, newText)
                .where(NOTES.ID.eq(id))
                .execute();
    }

    public void updateNotificationTime(long id, OffsetDateTime time) {
        context.update(NOTES)
                .set(NOTES.NOTIFICATION_TIME, time)
                .where(NOTES.ID.eq(id))
                .execute();
    }
}

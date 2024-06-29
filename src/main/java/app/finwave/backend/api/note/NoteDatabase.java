package app.finwave.backend.api.note;


import org.jooq.DSLContext;
import org.jooq.Record1;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.NotesRecord;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.when;
import static app.finwave.backend.jooq.Tables.NOTES;
import static app.finwave.backend.jooq.Tables.RECURRING_TRANSACTIONS;


public class NoteDatabase extends AbstractDatabase {

    public NoteDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newNote(int userId, OffsetDateTime notificationTime, String note) {
        return context.insertInto(NOTES)
                .set(NOTES.OWNER_ID, userId)
                .set(NOTES.NOTIFICATION_TIME, notificationTime)
                .set(NOTES.LAST_EDIT, OffsetDateTime.now())
                .set(NOTES.NOTE, note)
                .returningResult(NOTES.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public Optional<Long> newNote(int userId, String note) {
        return newNote(userId, null, note);
    }

    public List<NotesRecord> getToRemind(int count) {
        return context.selectFrom(NOTES)
                .where(NOTES.NOTIFICATION_TIME.lessOrEqual(OffsetDateTime.now()))
                .limit(count)
                .fetch();
    }

    public int getNotesCount(int userId) {
        return context.selectCount()
                .from(NOTES)
                .where(NOTES.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public Optional<NotesRecord> getNote(long id) {
        return context.selectFrom(NOTES)
                .where(NOTES.ID.eq(id))
                .fetchOptional();
    }

    public List<NotesRecord> getNotes(int userId) {
        return context.selectFrom(NOTES)
                .where(NOTES.OWNER_ID.eq(userId))
                .orderBy(NOTES.LAST_EDIT.desc(), NOTES.ID.desc())
                .fetch();
    }

    public List<NotesRecord> getImportantNotes(int userId) {
        return context.selectFrom(NOTES)
                .where(NOTES.OWNER_ID.eq(userId))
                .orderBy(
                        when(NOTES.NOTIFICATION_TIME.isNotNull().and(NOTES.NOTIFICATION_TIME.greaterOrEqual(currentOffsetDateTime())), NOTES.NOTIFICATION_TIME).asc(),
                        when(NOTES.NOTIFICATION_TIME.isNull().or(NOTES.NOTIFICATION_TIME.lessThan(currentOffsetDateTime())), NOTES.LAST_EDIT).desc()
                )
                .limit(10)
                .fetch();
    }

    public boolean userOwnNote(int userId, long noteId) {
        return context.select(NOTES.ID)
                .from(NOTES)
                .where(NOTES.OWNER_ID.eq(userId).and(NOTES.ID.eq(noteId)))
                .fetchOptional()
                .isPresent();
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

    public void deleteNote(long id) {
        context.deleteFrom(NOTES)
                .where(NOTES.ID.eq(id))
                .execute();
    }
}

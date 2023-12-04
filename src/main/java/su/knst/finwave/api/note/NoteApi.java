package su.knst.finwave.api.note;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.NotesConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.NotesRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class NoteApi {
    protected NotesConfig config;

    protected NoteDatabase database;

    @Inject
    public NoteApi(Configs configs, DatabaseWorker databaseWorker) {
        this.config = configs.getState(new NotesConfig());
        this.database = databaseWorker.get(NoteDatabase.class);
    }

    public Object newNote(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        Optional<OffsetDateTime> time = ParamsValidator
                .string(request, "notificationTime")
                .optional()
                .map(OffsetDateTime::parse);

        String text = ParamsValidator
                .string(request, "text")
                .length(1, config.maxNoteLength)
                .require();

        if (database.getNotesCount(sessionsRecord.getUserId()) >= config.maxNotesPerUser)
            halt(409);

        Optional<Long> noteId = time.isPresent() ?
                database.newNote(sessionsRecord.getUserId(), time.get(), text) :
                database.newNote(sessionsRecord.getUserId(), text);

        if (noteId.isEmpty())
            halt(500);

        response.status(201);

        return new NewNoteResponse(noteId.get());
    }

    public Object editNote(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long noteId = ParamsValidator
                .longV(request, "noteId")
                .matches((id) -> database.userOwnNote(sessionsRecord.getUserId(), id))
                .require();

        String text = ParamsValidator
                .string(request, "text")
                .length(1, config.maxNoteLength)
                .require();

        database.editNote(noteId, text);

        response.status(200);

        return ApiMessage.of("Note edited");
    }

    public Object editNoteNotificationTime(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        OffsetDateTime time = ParamsValidator
                .string(request, "notificationTime")
                .optional()
                .map(OffsetDateTime::parse)
                .orElse(null);

        long noteId = ParamsValidator
                .longV(request, "noteId")
                .matches((id) -> database.userOwnNote(sessionsRecord.getUserId(), id))
                .require();

        database.updateNotificationTime(noteId, time);

        response.status(200);

        return ApiMessage.of("Note notification time edited");
    }

    public Object getNote(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long noteId = ParamsValidator
                .longV(request, "noteId")
                .matches((id) -> database.userOwnNote(sessionsRecord.getUserId(), id))
                .require();

        Optional<NotesRecord> record = database.getNote(noteId);

        if (record.isEmpty())
            halt(404);

        response.status(200);

        return new GetNoteResponse(record.get());
    }

    public Object getNotesList(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<NotesRecord> records = database.getNotes(sessionsRecord.getUserId());

        response.status(200);

        return new GetNotesListResponse(records);
    }

    public Object getImportantNotes(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<NotesRecord> records = database.getImportantNotes(sessionsRecord.getUserId());

        response.status(200);

        return new GetNotesListResponse(records);
    }

    public Object deleteNote(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long noteId = ParamsValidator
                .longV(request, "noteId")
                .matches((id) -> database.userOwnNote(sessionsRecord.getUserId(), id))
                .require();

        database.deleteNote(noteId);

        response.status(200);

        return ApiMessage.of("Note deleted");
    }

    static class GetNotesListResponse extends ApiResponse {
        public final List<Entry> notes;

        public GetNotesListResponse(List<NotesRecord> records) {
            this.notes = records
                    .stream()
                    .map(v -> new Entry(v.getId(), v.getNotificationTime(), v.getLastEdit(), v.getNote()))
                    .toList();
        }

        record Entry(long noteId, OffsetDateTime notificationTime, OffsetDateTime lastEdit, String text) {}
    }

    static class GetNoteResponse extends ApiResponse {
        public final OffsetDateTime notificationTime;
        public final OffsetDateTime lastEdit;
        public final String text;

        public GetNoteResponse(NotesRecord record) {
            this.notificationTime = record.getNotificationTime();
            this.lastEdit = record.getLastEdit();
            this.text = record.getNote();
        }
    }

    static class NewNoteResponse extends ApiResponse {
        public final long noteId;

        public NewNoteResponse(long noteId) {
            this.noteId = noteId;
        }
    }
}

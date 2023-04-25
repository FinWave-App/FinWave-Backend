package su.knst.fintrack.api.note;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.ApiResponse;
import su.knst.fintrack.api.user.UserSettingsDatabase;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.app.NotesConfig;
import su.knst.fintrack.jooq.tables.records.NotesRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class NoteApi {
    protected NotesConfig config;

    protected NoteDatabase database;
    protected UserSettingsDatabase userSettingsDatabase;

    @Inject
    public NoteApi(Configs configs, NoteDatabase database, UserSettingsDatabase userSettingsDatabase) {
        this.config = configs.getState(new NotesConfig());
        this.database = database;
        this.userSettingsDatabase = userSettingsDatabase;
    }

    public Object newNote(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");
        OffsetDateTime time = ParamsValidator
                .string(request, "notification_time")
                .optional()
                .map(OffsetDateTime::parse)
                .orElse(null);

        String text = ParamsValidator
                .string(request, "text")
                .length(1, config.maxNoteLength)
                .require();

        Optional<Long> noteId = database.newNote(sessionsRecord.getUserId(), time, text);

        if (noteId.isEmpty())
            halt(500);

        response.status(201);

        return new NewNoteResponse(noteId.get());
    }

    public Object editNote(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String text = ParamsValidator
                .string(request, "text")
                .length(1, config.maxNoteLength)
                .require();

        long noteId = ParamsValidator
                .longV(request, "noteId")
                .matches((id) -> database.userOwnNote(sessionsRecord.getUserId(), id))
                .require();

        database.editNote(noteId, text);

        response.status(200);

        return null;
    }

    public Object editNoteNotificationTime(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        OffsetDateTime time = ParamsValidator
                .string(request, "notification_time")
                .optional()
                .map(OffsetDateTime::parse)
                .orElse(null);

        long noteId = ParamsValidator
                .longV(request, "noteId")
                .matches((id) -> database.userOwnNote(sessionsRecord.getUserId(), id))
                .require();

        database.updateNotificationTime(noteId, time);

        response.status(200);

        return null;
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

        int offset = ParamsValidator
                .integer(request, "offset")
                .range(0, Integer.MAX_VALUE)
                .require();

        int count = ParamsValidator
                .integer(request, "count")
                .range(1, config.maxNotesInListPerRequest)
                .require();

        List<NotesRecord> records = database.getNotes(sessionsRecord.getUserId(), offset, count);

        response.status(200);

        return new GetNotesListResponse(records);
    }

    public Object findNote(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        int offset = ParamsValidator
                .integer(request, "offset")
                .range(0, Integer.MAX_VALUE)
                .require();

        int count = ParamsValidator
                .integer(request, "count")
                .range(1, config.maxNotesInListPerRequest)
                .require();

        String filter = ParamsValidator
                .string(request, "filter")
                .length(config.minFilterLength, config.maxNoteLength)
                .require();

        List<NotesRecord> records = database.findNote(sessionsRecord.getUserId(), filter, offset, count);

        response.status(200);

        return new GetNotesListResponse(records);
    }

    static class GetNotesListResponse extends ApiResponse {
        public final List<Entry> notes;

        public GetNotesListResponse(List<NotesRecord> records) {
            this.notes = records
                    .stream()
                    .map(v -> new Entry(v.getId(), v.getNotificationTime(), v.getNote()))
                    .toList();
        }

        record Entry(long id, OffsetDateTime notificationTime, String text) {
        }
    }

    static class GetNoteResponse extends ApiResponse {
        public final OffsetDateTime notificationTime;
        public final String text;

        public GetNoteResponse(NotesRecord record) {
            this.notificationTime = record.getNotificationTime();
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

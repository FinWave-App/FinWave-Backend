package app.finwave.backend.api.account.folder;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import app.finwave.backend.jooq.tables.records.AccountsFoldersRecord;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.AccountsConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AccountFolderApi {
    protected AccountsConfig config;
    protected AccountFolderDatabase database;
    protected WebSocketWorker socketWorker;

    @Inject
    public AccountFolderApi(DatabaseWorker databaseWorker, Configs configs, WebSocketWorker socketWorker) {
        this.database = databaseWorker.get(AccountFolderDatabase.class);
        this.config = configs.getState(new AccountsConfig());

        this.socketWorker = socketWorker;
    }

    public Object newFolder(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.folders.maxNameLength)
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.folders.maxDescriptionLength)
                .optional();

        if (database.getFolderCount(sessionsRecord.getUserId()) >= config.folders.maxFoldersPerUser)
            halt(409);

        Optional<Long> folderId = database.newFolder(sessionsRecord.getUserId(), name, description.orElse(null));

        if (folderId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountFolders"));

        response.status(201);

        return new NewFolderResponse(folderId.get());
    }

    public Object getFolders(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<AccountsFoldersRecord> records = database.getFolders(sessionsRecord.getUserId());

        response.status(200);

        return new GetFoldersResponse(records);
    }

    public Object editFolderName(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.folders.maxNameLength)
                .require();

        long folderId = ParamsValidator
                .longV(request, "folderId")
                .matches((id) -> database.userOwnFolder(sessionsRecord.getUserId(), id))
                .require();

        database.editFolderName(folderId, name);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountFolders"));

        response.status(200);

        return ApiMessage.of("Folder name edited");
    }

    public Object editFolderDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.folders.maxDescriptionLength)
                .optional();

        long folderId = ParamsValidator
                .longV(request, "folderId")
                .matches((id) -> database.userOwnFolder(sessionsRecord.getUserId(), id))
                .require();

        database.editFolderDescription(folderId, description.orElse(null));

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountFolders"));

        response.status(200);

        return ApiMessage.of("Folder description edited");
    }

    public Object deleteFolder(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long folderId = ParamsValidator
                .longV(request, "folderId")
                .matches((id) -> database.userOwnFolder(sessionsRecord.getUserId(), id))
                .matches((id) -> database.folderSafeToDelete(id))
                .require();

        database.deleteFolder(folderId);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accountFolders"));

        response.status(200);

        return ApiMessage.of("Folder deleted");
    }

    static class GetFoldersResponse extends ApiResponse {
        public final List<Entry> folders;

        public GetFoldersResponse(List<AccountsFoldersRecord> records) {
            this.folders = records
                    .stream()
                    .map((r) -> new Entry(r.getId(), r.getName(), r.getDescription()))
                    .toList();
        }

        record Entry(long folderId, String name, String description) {}
    }

    static class NewFolderResponse extends ApiResponse {
        public final long folderId;

        public NewFolderResponse(long folderId) {
            this.folderId = folderId;
        }
    }
}

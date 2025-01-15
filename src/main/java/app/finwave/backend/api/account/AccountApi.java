package app.finwave.backend.api.account;

import app.finwave.backend.api.accumulation.AccumulationApi;
import app.finwave.backend.api.accumulation.AccumulationDatabase;
import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import app.finwave.backend.api.recurring.RecurringTransactionDatabase;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.account.folder.AccountFolderDatabase;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.AccountsConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.AccountsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AccountApi {
    protected AccountDatabase database;
    protected AccountFolderDatabase folderDatabase;
    protected CurrencyDatabase currencyDatabase;

    protected AccountsConfig config;

    protected WebSocketWorker socketWorker;

    protected TransactionsManager transactionsManager;

    protected RecurringTransactionDatabase recurringTransactionDatabase;
    protected AccumulationDatabase accumulationDatabase;

    @Inject
    public AccountApi(DatabaseWorker databaseWorker, Configs configs, WebSocketWorker socketWorker, TransactionsManager manager) {
        this.database = databaseWorker.get(AccountDatabase.class);
        this.folderDatabase = databaseWorker.get(AccountFolderDatabase.class);
        this.currencyDatabase = databaseWorker.get(CurrencyDatabase.class);

        this.config = configs.getState(new AccountsConfig());

        this.socketWorker = socketWorker;
        this.transactionsManager = manager;
        this.recurringTransactionDatabase = databaseWorker.get(RecurringTransactionDatabase.class);
        this.accumulationDatabase = databaseWorker.get(AccumulationDatabase.class);
    }

    public Object newAccount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long folderId = ParamsValidator
                .longV(request, "folderId")
                .matches((id) -> folderDatabase.userOwnFolder(sessionsRecord.getUserId(), id))
                .require();

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> currencyDatabase.userCanReadCurrency(sessionsRecord.getUserId(), id))
                .require();

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.maxNameLength)
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        if (database.getAccountsCount(sessionsRecord.getUserId()) >= config.maxAccountsPerUser)
            halt(409);

        Optional<Long> accountId = database
                .newAccount(sessionsRecord.getUserId(), folderId, currencyId, name, description.orElse(null));

        if (accountId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accounts"));

        response.status(201);

        return new NewAccountResponse(accountId.get());
    }

    public Object getAccounts(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<AccountsRecord> records = database.getAccounts(sessionsRecord.getUserId());

        response.status(200);

        return new GetAccountsListResponse(records);
    }

    public Object hideAccount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        database.hideAccount(accountId);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accounts"));

        response.status(200);

        return ApiMessage.of("Account hided");
    }

    public Object showAccount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        database.showAccount(accountId);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accounts"));

        response.status(200);

        return ApiMessage.of("Account showed");
    }

    public Object editAccountName(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.maxNameLength)
                .require();

        database.editAccountName(accountId, name);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accounts"));

        response.status(200);

        return ApiMessage.of("Account name edited");
    }

    public Object editAccountDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        database.editAccountDescription(accountId, description.orElse(null));

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accounts"));

        response.status(200);

        return ApiMessage.of("Account description edited");
    }

    public Object editAccountFolder(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        long folderId = ParamsValidator
                .longV(request, "folderId")
                .matches((id) -> folderDatabase.userOwnFolder(sessionsRecord.getUserId(), id))
                .require();

        database.editAccountFolder(accountId, folderId);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accounts"));

        response.status(200);

        return ApiMessage.of("Account folder edited");
    }

    public Object deleteAccount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        if (recurringTransactionDatabase.accountAffected(accountId)) {
            response.status(400);

            return ApiMessage.of("Some recurring transaction affects to account");
        }

        if (accumulationDatabase.accountAffected(accountId)) {
            response.status(400);

            return ApiMessage.of("Some accumulation settings affects to account");
        }

        if (transactionsManager.getTransactionsCount(sessionsRecord.getUserId(), TransactionsFilter.EMPTY.setAccountIds(List.of(accountId))) > 0) {
            response.status(400);

            return ApiMessage.of("Some transactions affects to account");
        }

        database.deleteAccount(accountId);
        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("accounts"));

        response.status(200);

        return ApiMessage.of("Account deleted");
    }

    static class GetAccountsListResponse extends ApiResponse {
        public final List<Entry> accounts;

        public GetAccountsListResponse(List<AccountsRecord> records) {
            this.accounts = records
                    .stream()
                    .map(v -> new Entry(
                            v.getId(),
                            v.getFolderId(),
                            v.getCurrencyId(),
                            v.getAmount(),
                            v.getHidden(),
                            v.getName(),
                            v.getDescription()))
                    .toList();
        }

        record Entry(long accountId, long folderId, long currencyId, BigDecimal amount, boolean hidden, String name, String description) {}
    }

    static class NewAccountResponse extends ApiResponse {
        public final long accountId;

        public NewAccountResponse(long accountId) {
            this.accountId = accountId;
        }
    }
}
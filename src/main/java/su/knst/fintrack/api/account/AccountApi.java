package su.knst.fintrack.api.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.ApiResponse;
import su.knst.fintrack.api.account.tag.AccountTagDatabase;
import su.knst.fintrack.api.currency.CurrencyDatabase;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.app.AccountsConfig;
import su.knst.fintrack.http.ApiMessage;
import su.knst.fintrack.jooq.tables.records.AccountsRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class AccountApi {
    protected AccountDatabase database;
    protected AccountTagDatabase tagDatabase;
    protected CurrencyDatabase currencyDatabase;

    protected AccountsConfig config;

    @Inject
    public AccountApi(AccountDatabase database, AccountTagDatabase tagDatabase, CurrencyDatabase currencyDatabase, Configs configs) {
        this.database = database;
        this.tagDatabase = tagDatabase;
        this.currencyDatabase = currencyDatabase;
        this.config = configs.getState(new AccountsConfig());
    }

    public Object newAccount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
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
                .newAccount(sessionsRecord.getUserId(), tagId, currencyId, name, description.orElse(null));

        if (accountId.isEmpty())
            halt(500);

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

        response.status(200);

        database.hideAccount(accountId);

        return ApiMessage.of("Account hided");
    }

    public Object showAccount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        response.status(200);

        database.showAccount(accountId);

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

        response.status(200);

        return ApiMessage.of("Account name edited");
    }

    public Object editAccountDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .require();

        database.editAccountDescription(accountId, description);

        response.status(200);

        return ApiMessage.of("Account description edited");
    }

    public Object editAccountTag(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> database.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        database.editAccountTag(accountId, tagId);

        response.status(200);

        return ApiMessage.of("Account tag edited");
    }

    static class GetAccountsListResponse extends ApiResponse {
        public final List<Entry> accounts;

        public GetAccountsListResponse(List<AccountsRecord> records) {
            this.accounts = records
                    .stream()
                    .map(v -> new Entry(
                            v.getId(),
                            v.getTagId(),
                            v.getCurrencyId(),
                            v.getAmount(),
                            v.getHidden(),
                            v.getName(),
                            v.getDescription()))
                    .toList();
        }

        record Entry(long id, long tagId, long currencyId, BigDecimal amount, boolean hidden, String name, String description) {}
    }

    static class NewAccountResponse extends ApiResponse {
        public final long accountId;

        public NewAccountResponse(long accountId) {
            this.accountId = accountId;
        }
    }
}
package su.knst.fintrack.api.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.ApiResponse;
import su.knst.fintrack.api.account.AccountDatabase;
import su.knst.fintrack.api.transaction.filter.TransactionsFilter;
import su.knst.fintrack.api.transaction.tag.TransactionTagDatabase;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.app.TransactionConfig;
import su.knst.fintrack.http.ApiMessage;
import su.knst.fintrack.jooq.tables.records.TransactionsRecord;
import su.knst.fintrack.jooq.tables.records.UsersSessionsRecord;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class TransactionApi {
    protected TransactionDatabase database;
    protected TransactionTagDatabase tagDatabase;
    protected AccountDatabase accountDatabase;
    protected TransactionConfig config;

    @Inject
    public TransactionApi(TransactionDatabase database, TransactionTagDatabase tagDatabase, AccountDatabase accountDatabase, Configs configs) {
        this.config = configs.getState(new TransactionConfig());
        this.database = database;
        this.tagDatabase = tagDatabase;
        this.accountDatabase = accountDatabase;
    }

    public Object newTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        OffsetDateTime time = ParamsValidator
                .string(request, "createdAt")
                .map(OffsetDateTime::parse);

        BigDecimal delta = ParamsValidator
                .string(request, "delta")
                .map(BigDecimal::new);

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        long transactionId = database.applyTransaction(
                sessionsRecord.getUserId(),
                tagId,
                accountId,
                time,
                delta,
                description.orElse(null)
        );

        response.status(201);

        return new NewTransactionResponse(transactionId);
    }

    public Object deleteTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long transactionId = ParamsValidator
                .longV(request, "transactionId")
                .matches((id) -> database.userOwnTransaction(sessionsRecord.getUserId(), id))
                .require();

        database.cancelTransaction(transactionId);

        response.status(200);

        return ApiMessage.of("Transaction deleted");
    }

    public Object editTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long transactionId = ParamsValidator
                .longV(request, "transactionId")
                .matches((id) -> database.userOwnTransaction(sessionsRecord.getUserId(), id))
                .require();

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        OffsetDateTime time = ParamsValidator
                .string(request, "createdAt")
                .map(OffsetDateTime::parse);

        BigDecimal delta = ParamsValidator
                .string(request, "delta")
                .map(BigDecimal::new);

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        database.editTransaction(transactionId, tagId, accountId, time, delta, description.orElse(null));

        response.status(200);

        return ApiMessage.of("Transaction edited");
    }

    public Object getTransactionsCount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        TransactionsFilter filter = new TransactionsFilter(request);

        int count = database.getTransactionsCount(sessionsRecord.getUserId(), filter);

        response.status(200);

        return new TransactionsCountResponse(count);
    }

    public Object getTransactions(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        int offset = ParamsValidator
                .integer(request, "offset")
                .range(0, Integer.MAX_VALUE)
                .require();

        int count = ParamsValidator
                .integer(request, "count")
                .range(1, config.maxTransactionsInListPerRequest)
                .require();

        TransactionsFilter filter = new TransactionsFilter(request);

        List<TransactionsRecord> records = database.getTransactions(sessionsRecord.getUserId(), offset, count, filter);

        response.status(200);

        return new GetTransactionsListResponse(records);
    }

    static class GetTransactionsListResponse extends ApiResponse {
        public final List<Entity> transactions;

        public GetTransactionsListResponse(List<TransactionsRecord> transactions) {
            this.transactions = transactions.stream()
                    .map(r -> new Entity(
                            r.getId(),
                            r.getTagId(),
                            r.getAccountId(),
                            r.getCurrencyId(),
                            r.getCreatedAt(),
                            r.getDelta(),
                            r.getDescription()))
                    .toList();
        }

        record Entity(long transactionId, long tagId, long accountId, long currencyId, OffsetDateTime createdAt, BigDecimal delta, String description) {}
    }

    static class TransactionsCountResponse extends ApiResponse {
        public final int count;

        public TransactionsCountResponse(int count) {
            this.count = count;
        }
    }

    static class NewTransactionResponse extends ApiResponse {
        public final long transactionId;

        public NewTransactionResponse(long transactionId) {
            this.transactionId = transactionId;
        }
    }
}

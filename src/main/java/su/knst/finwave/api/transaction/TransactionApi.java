package su.knst.finwave.api.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Record;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.account.AccountDatabase;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.api.transaction.generator.TransactionEntry;
import su.knst.finwave.api.transaction.generator.TransactionsListGenerator;
import su.knst.finwave.api.transaction.tag.TransactionTagDatabase;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.TransactionConfig;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.TransactionsRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.InvalidParameterException;
import su.knst.finwave.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS;
import static su.knst.finwave.jooq.Tables.TRANSACTIONS_METADATA;

@Singleton
public class TransactionApi {
    protected TransactionDatabase database;
    protected TransactionTagDatabase tagDatabase;
    protected AccountDatabase accountDatabase;
    protected TransactionsListGenerator generator;
    protected TransactionConfig config;

    @Inject
    public TransactionApi(TransactionDatabase database, TransactionTagDatabase tagDatabase, AccountDatabase accountDatabase, TransactionsListGenerator generator, Configs configs) {
        this.config = configs.getState(new TransactionConfig());
        this.database = database;
        this.tagDatabase = tagDatabase;
        this.accountDatabase = accountDatabase;
        this.generator = generator;
    }

    public Object newInternalTransfer(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        long fromAccountId = ParamsValidator
                .longV(request, "fromAccountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        long toAccountId = ParamsValidator
                .longV(request, "toAccountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .matches((id) -> id != fromAccountId)
                .require();

        OffsetDateTime time = ParamsValidator
                .string(request, "createdAt")
                .map(OffsetDateTime::parse);

        BigDecimal fromDelta = ParamsValidator
                .string(request, "fromDelta")
                .map(BigDecimal::new);

        if (fromDelta.signum() >= 0)
            throw new InvalidParameterException("fromDelta cannot be greater than or equal to zero");

        BigDecimal toDelta = ParamsValidator
                .string(request, "toDelta")
                .map(BigDecimal::new);

        if (toDelta.signum() <= 0)
            throw new InvalidParameterException("toDelta cannot be less than or equal to zero");

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        long transactionId = database.applyInternalTransfer(
                sessionsRecord.getUserId(),
                tagId,
                fromAccountId,
                toAccountId,
                time,
                fromDelta,
                toDelta,
                description.orElse(null)
        );

        response.status(201);

        return new NewTransactionResponse(transactionId);
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

        List<Record> records = database.getTransactions(sessionsRecord.getUserId(), offset, count, filter);
        List<TransactionEntry<?>> transactions = generator.prepareTransactions(records);

        response.status(200);

        return new GetTransactionsListResponse(transactions);
    }

    static class GetTransactionsListResponse extends ApiResponse {
        public final List<TransactionEntry<?>> transactions;

        public GetTransactionsListResponse(List<TransactionEntry<?>> transactions) {
            this.transactions = transactions;
        }
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

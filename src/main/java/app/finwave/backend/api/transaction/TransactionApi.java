package app.finwave.backend.api.transaction;

import app.finwave.backend.api.category.CategoryDatabase;
import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import app.finwave.backend.jooq.tables.records.CategoriesRecord;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.account.AccountDatabase;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.api.transaction.manager.data.TransactionEntry;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.transaction.manager.records.BulkTransactionsRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewInternalRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.TransactionConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class TransactionApi {
    protected TransactionsManager manager;
    protected CategoryDatabase categoryDatabase;
    protected AccountDatabase accountDatabase;
    protected TransactionConfig config;

    protected WebSocketWorker socketWorker;

    @Inject
    public TransactionApi(TransactionsManager manager, DatabaseWorker databaseWorker, Configs configs, WebSocketWorker socketWorker) {
        this.config = configs.getState(new TransactionConfig());
        this.manager = manager;
        this.categoryDatabase = databaseWorker.get(CategoryDatabase.class);
        this.accountDatabase = databaseWorker.get(AccountDatabase.class);

        this.socketWorker = socketWorker;
    }

    public Object newBulkTransactions(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        BulkTransactionsRecord args = ParamsValidator.bodyObject(request, BulkTransactionsRecord.class)
                .matches((r) -> !r.entries().isEmpty())
                .require();

        args.entries().forEach((entry) -> {
            var validator = ParamsValidator.bodyObject(entry)
                    .matches((e) -> e.type == 0 || e.type == 1, "type")
                    .matches((e) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), e.categoryId), "categoryId")
                    .matches((e) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), e.accountId), "accountId")
                    .matches((e) -> e.created != null, "created")
                    .matches((e) -> e.delta != null, "delta")
                    .matches((e) -> e.description == null || !e.description.isBlank() && e.description.length() <= config.maxDescriptionLength, "description");

            validator.require();

            if (entry.type == 0) {
                Optional<CategoriesRecord> category = categoryDatabase.getCategory(entry.categoryId);

                if (category.isEmpty())
                    halt(500);

                if (category.get().getType() != 0 && entry.delta.signum() != category.get().getType())
                    entry.delta = entry.delta.negate();

                return;
            }

            validator
                    .matches((e) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), e.toAccountId), "toAccountId")
                    .matches((e) -> e.toDelta != null, "toDelta")
                    .require();

            if (entry.delta.signum() >= 0)
                throw new InvalidParameterException("fromDelta cannot be greater than or equal to zero");

            if (entry.toDelta.signum() <= 0)
                throw new InvalidParameterException("toDelta cannot be less than or equal to zero");
        });

        manager.applyBulkTransactions(args, sessionsRecord.getUserId());

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactions"));

        response.status(201);

        return ApiMessage.of("Successful");
    }

    public Object newInternalTransfer(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), id))
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
                .optional()
                .map(OffsetDateTime::parse)
                .orElseGet(OffsetDateTime::now);

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

        long transactionId = manager.applyInternalTransfer(new TransactionNewInternalRecord(
                sessionsRecord.getUserId(),
                categoryId,
                fromAccountId,
                toAccountId,
                time,
                fromDelta,
                toDelta,
                description.orElse(null))
        );

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactions"));

        response.status(201);

        return new NewTransactionResponse(transactionId);
    }

    public Object newTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        OffsetDateTime time = ParamsValidator
                .string(request, "createdAt")
                .optional()
                .map(OffsetDateTime::parse)
                .orElseGet(OffsetDateTime::now);

        BigDecimal delta = ParamsValidator
                .string(request, "delta")
                .map(BigDecimal::new);

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        Optional<CategoriesRecord> category = categoryDatabase.getCategory(categoryId);

        if (category.isEmpty())
            halt(500);

        if (category.get().getType() != 0 && delta.signum() != category.get().getType())
            delta = delta.negate();


        long transactionId = manager.applyTransaction(new TransactionNewRecord(
                sessionsRecord.getUserId(),
                categoryId,
                accountId,
                time,
                delta,
                description.orElse(null)
        ));

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactions"));

        response.status(201);

        return new NewTransactionResponse(transactionId);
    }

    public Object deleteTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long transactionId = ParamsValidator
                .longV(request, "transactionId")
                .matches((id) -> manager.userOwnTransaction(sessionsRecord.getUserId(), id))
                .require();

        manager.cancelTransaction(transactionId);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactions"));

        response.status(200);

        return ApiMessage.of("Transaction deleted");
    }

    public Object editTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long transactionId = ParamsValidator
                .longV(request, "transactionId")
                .matches((id) -> manager.userOwnTransaction(sessionsRecord.getUserId(), id))
                .require();

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        OffsetDateTime time = ParamsValidator
                .string(request, "createdAt")
                .optional()
                .map(OffsetDateTime::parse)
                .orElse(null);

        BigDecimal delta = ParamsValidator
                .string(request, "delta")
                .map(BigDecimal::new);

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        Optional<CategoriesRecord> category = categoryDatabase.getCategory(categoryId);

        if (category.isEmpty())
            halt(500);

        if (category.get().getType() != 0 && delta.signum() != category.get().getType())
            delta = delta.negate();

        manager.editTransaction(transactionId, new TransactionEditRecord(categoryId, accountId, time, delta, description.orElse(null)));

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("transactions"));

        response.status(200);

        return ApiMessage.of("Transaction edited");
    }

    public Object getTransactionsCount(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        TransactionsFilter filter = new TransactionsFilter(request);

        int count = manager.getTransactionsCount(sessionsRecord.getUserId(), filter);

        response.status(200);

        return new TransactionsCountResponse(count);
    }

    public Object getTransactions(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        int offset = ParamsValidator
                .integer(request, "offset")
                .range(0, Integer.MAX_VALUE)
                .optional()
                .orElse(0);

        int count = ParamsValidator
                .integer(request, "count")
                .range(1, config.maxTransactionsInListPerRequest)
                .optional()
                .orElse(10);
        TransactionsFilter filter = new TransactionsFilter(request);

        List<TransactionEntry<?>> transactions = manager.getTransactions(sessionsRecord.getUserId(), offset, count, filter);

        response.status(200);

        return new GetTransactionsListResponse(transactions);
    }

    public static class GetTransactionsListResponse extends ApiResponse {
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

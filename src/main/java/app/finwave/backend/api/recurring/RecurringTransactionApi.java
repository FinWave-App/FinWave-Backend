package app.finwave.backend.api.recurring;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.account.AccountDatabase;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.category.CategoryDatabase;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.RecurringTransactionConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.RecurringTransactionsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.service.recurring.NextRepeatTools;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class RecurringTransactionApi {

    protected RecurringTransactionDatabase database;
    protected CategoryDatabase categoryDatabase;
    protected AccountDatabase accountDatabase;
    protected RecurringTransactionConfig config;
    protected TransactionsManager manager;

    protected WebSocketWorker socketWorker;

    @Inject
    public RecurringTransactionApi(DatabaseWorker databaseWorker, TransactionsManager manager, Configs configs, WebSocketWorker socketWorker) {
        this.database = databaseWorker.get(RecurringTransactionDatabase.class);
        this.categoryDatabase = databaseWorker.get(CategoryDatabase.class);
        this.accountDatabase = databaseWorker.get(AccountDatabase.class);

        this.manager = manager;

        this.config = configs.getState(new RecurringTransactionConfig());

        this.socketWorker = socketWorker;
    }

    public Object newRecurringTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        OffsetDateTime nextRepeat = ParamsValidator
                .string(request, "nextRepeat")
                .map(OffsetDateTime::parse);

        if (nextRepeat.isBefore(OffsetDateTime.now()))
            throw new InvalidParameterException("nextRepeat");

        RepeatType repeatType = ParamsValidator
                .integer(request, "repeatType")
                .range(0, RepeatType.values().length - 1)
                .map((i) -> RepeatType.values()[i]);

        short repeatArg = ParamsValidator
                .integer(request, "repeatArg")
                .matches((i) -> NextRepeatTools.validateArg(repeatType, i.shortValue()))
                .map(Integer::shortValue);

        NotificationMode notificationMode = ParamsValidator
                .integer(request, "notificationMode")
                .range(0, NotificationMode.values().length - 1)
                .map((i) -> NotificationMode.values()[i]);

        BigDecimal delta = ParamsValidator
                .string(request, "delta")
                .map(BigDecimal::new);

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        long recurringId = database.newRecurring(
                sessionsRecord.getUserId(),
                categoryId,
                accountId,
                repeatType,
                repeatArg,
                notificationMode,
                nextRepeat,
                delta,
                description.orElse(null)
        ).orElseThrow();

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("recurringTransactions"));

        response.status(201);

        return new NewRecurringTransactionResponse(recurringId);
    }

    public Object editRecurringTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long recurringId = ParamsValidator
                .longV(request, "recurringTransactionId")
                .matches((id) -> database.userOwnRecurringTransaction(sessionsRecord.getUserId(), id))
                .require();

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        OffsetDateTime nextRepeat = ParamsValidator
                .string(request, "nextRepeat")
                .map(OffsetDateTime::parse);

        if (nextRepeat.isBefore(OffsetDateTime.now()))
            throw new InvalidParameterException("nextRepeat");

        RepeatType repeatType = ParamsValidator
                .integer(request, "repeatType")
                .range(0, RepeatType.values().length - 1)
                .map((i) -> RepeatType.values()[i]);

        short repeatArg = ParamsValidator
                .integer(request, "repeatArg")
                .matches((i) -> NextRepeatTools.validateArg(repeatType, i.shortValue()))
                .map(Integer::shortValue);

        NotificationMode notificationMode = ParamsValidator
                .integer(request, "notificationMode")
                .range(0, NotificationMode.values().length - 1)
                .map((i) -> NotificationMode.values()[i]);

        BigDecimal delta = ParamsValidator
                .string(request, "delta")
                .map(BigDecimal::new);

        Optional<String> description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .optional();

        database.editRecurring(
                recurringId,
                categoryId,
                accountId,
                repeatType,
                repeatArg,
                notificationMode,
                nextRepeat,
                delta,
                description.orElse(null)
        );

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("recurringTransactions"));

        response.status(200);

        return ApiMessage.of("Recurring transaction edited");
    }

    public Object deleteRecurringTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long recurringId = ParamsValidator
                .longV(request, "recurringId")
                .matches((id) -> database.userOwnRecurringTransaction(sessionsRecord.getUserId(), id))
                .require();

        database.deleteRecurring(recurringId);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("recurringTransactions"));

        response.status(200);

        return ApiMessage.of("Recurring deleted");
    }

    public Object getList(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<RecurringTransactionsRecord> records = database.getList(sessionsRecord.getUserId());

        response.status(200);

        return new GetRecurringListResponse(records);
    }

    static class NewRecurringTransactionResponse extends ApiResponse {
        public final long recurringTransactionId;

        public NewRecurringTransactionResponse(long recurringTransactionId) {
            this.recurringTransactionId = recurringTransactionId;
        }
    }

    static class GetRecurringListResponse extends ApiResponse {
        public final List<Entry> recurringList;

        public GetRecurringListResponse(List<RecurringTransactionsRecord> recurringList) {
            this.recurringList = recurringList.stream()
                    .map(Entry::new)
                    .toList();
        }

        static class Entry {
            public final long recurringTransactionId;
            public final long categoryId;
            public final long accountId;
            public final long currencyId;
            public final OffsetDateTime lastRepeat;
            public final OffsetDateTime nextRepeat;
            public final short repeatType;
            public final short repeatArg;
            public final int notificationMode;
            public final BigDecimal delta;
            public final String description;

            public Entry(RecurringTransactionsRecord record) {
                this(record.getId(),
                        record.getCategoryId(),
                        record.getAccountId(),
                        record.getCurrencyId(),
                        record.getLastRepeat(),
                        record.getNextRepeat(),
                        record.getRepeatFunc(),
                        record.getRepeatFuncArg(),
                        record.getNotificationMode(),
                        record.getDelta(),
                        record.getDescription()
                );
            }

            public Entry(long recurringTransactionId, long categoryId, long accountId, long currencyId, OffsetDateTime lastRepeat, OffsetDateTime nextRepeat, short repeatType, short repeatArg, int notificationMode, BigDecimal delta, String description) {
                this.recurringTransactionId = recurringTransactionId;
                this.categoryId = categoryId;
                this.accountId = accountId;
                this.currencyId = currencyId;
                this.lastRepeat = lastRepeat;
                this.nextRepeat = nextRepeat;
                this.repeatType = repeatType;
                this.repeatArg = repeatArg;
                this.notificationMode = notificationMode;
                this.delta = delta;
                this.description = description;
            }
        }
    }
}

package su.knst.finwave.api.transaction.recurring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.account.AccountDatabase;
import su.knst.finwave.api.transaction.manager.TransactionsManager;
import su.knst.finwave.api.transaction.tag.TransactionTagDatabase;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.RecurringTransactionConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.RecurringTransactionsRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.service.recurring.NextRepeatTools;
import su.knst.finwave.utils.params.InvalidParameterException;
import su.knst.finwave.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class RecurringTransactionApi {

    protected RecurringTransactionDatabase database;
    protected TransactionTagDatabase tagDatabase;
    protected AccountDatabase accountDatabase;
    protected RecurringTransactionConfig config;
    protected TransactionsManager manager;

    @Inject
    public RecurringTransactionApi(DatabaseWorker databaseWorker, TransactionsManager manager, Configs configs) {
        this.database = databaseWorker.get(RecurringTransactionDatabase.class);
        this.tagDatabase = databaseWorker.get(TransactionTagDatabase.class);
        this.accountDatabase = databaseWorker.get(AccountDatabase.class);

        this.manager = manager;

        this.config = configs.getState(new RecurringTransactionConfig());
    }

    public Object newRecurringTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
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
                tagId,
                accountId,
                repeatType,
                repeatArg,
                notificationMode,
                nextRepeat,
                delta,
                description.orElse(null)
        ).orElseThrow();

        response.status(201);

        return new NewRecurringTransactionResponse(recurringId);
    }

    public Object editRecurringTransaction(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long recurringId = ParamsValidator
                .longV(request, "recurringTransactionId")
                .matches((id) -> database.userOwnRecurringTransaction(sessionsRecord.getUserId(), id))
                .require();

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
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
                tagId,
                accountId,
                repeatType,
                repeatArg,
                notificationMode,
                nextRepeat,
                delta,
                description.orElse(null)
        );

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
            public final long tagId;
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
                        record.getTagId(),
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

            public Entry(long recurringTransactionId, long tagId, long accountId, long currencyId, OffsetDateTime lastRepeat, OffsetDateTime nextRepeat, short repeatType, short repeatArg, int notificationMode, BigDecimal delta, String description) {
                this.recurringTransactionId = recurringTransactionId;
                this.tagId = tagId;
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

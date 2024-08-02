package app.finwave.backend.api.transaction.tag.managment;

import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.account.AccountApi;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import app.finwave.backend.api.transaction.tag.TransactionTagDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.AccountsRecord;
import app.finwave.backend.jooq.tables.records.TransactionsTagsManagementRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class TagManagementApi {
    protected TagManagementManager manager;
    protected WebSocketWorker socketWorker;
    protected TransactionTagDatabase tagDatabase;
    protected CurrencyDatabase currencyDatabase;

    @Inject
    public TagManagementApi(TagManagementManager manager, WebSocketWorker socketWorker, DatabaseWorker databaseWorker) {
        this.manager = manager;
        this.socketWorker = socketWorker;
        this.tagDatabase = databaseWorker.get(TransactionTagDatabase.class);
        this.currencyDatabase = databaseWorker.get(CurrencyDatabase.class);
    }

    public Object addManagement(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> currencyDatabase.userCanReadCurrency(sessionsRecord.getUserId(), id))
                .require();

        if (manager.managementExists(sessionsRecord.getUserId(), tagId, currencyId, -1))
            throw new InvalidParameterException("management with that tag and currency already exists");

        short dateType = ParamsValidator
                .integer(request, "dateType")
                .range(0, 1)
                .require()
                .shortValue();

        BigDecimal amount = ParamsValidator
                .string(request, "amount")
                .map(BigDecimal::new);

        if (amount.signum() == 0)
            throw new InvalidParameterException("amount cannot be equal to zero");

        Optional<Long> newId = manager.add(sessionsRecord.getUserId(), tagId, currencyId, dateType, amount);

        if (newId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("tagManagement"));

        response.status(201);

        return new NewManagementResponse(newId.get());
    }

    public Object editManagement(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long managementId = ParamsValidator
                .longV(request, "managementId")
                .matches((id) -> manager.userOwnManagement(sessionsRecord.getUserId(), id))
                .require();

        long tagId = ParamsValidator
                .longV(request, "tagId")
                .matches((id) -> tagDatabase.userOwnTag(sessionsRecord.getUserId(), id))
                .require();

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> currencyDatabase.userCanReadCurrency(sessionsRecord.getUserId(), id))
                .require();

        if (manager.managementExists(sessionsRecord.getUserId(), tagId, currencyId, managementId))
            throw new InvalidParameterException("management with that tag and currency already exists");

        short dateType = ParamsValidator
                .integer(request, "dateType")
                .range(0, 1)
                .require()
                .shortValue();

        BigDecimal amount = ParamsValidator
                .string(request, "amount")
                .map(BigDecimal::new);

        if (amount.signum() == 0)
            throw new InvalidParameterException("amount cannot be equal to zero");

        manager.update(sessionsRecord.getUserId(), managementId, tagId, currencyId, dateType, amount);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("tagManagement"));

        response.status(200);

        return ApiMessage.of("Tag management edited");
    }

    public Object getSettings(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        response.status(200);

        return new GetListResponse(manager.getSettings(sessionsRecord.getUserId()));
    }

    public Object remove(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long managementId = ParamsValidator
                .longV(request, "managementId")
                .matches((id) -> manager.userOwnManagement(sessionsRecord.getUserId(), id))
                .require();

        boolean result = manager.remove(managementId);

        if (!result)
            halt(500);

        response.status(200);

        return ApiMessage.of("Tag management removed");
    }

    static class GetListResponse extends ApiResponse {
        public final List<Entry> managements;

        public GetListResponse(List<TransactionsTagsManagementRecord> records) {
            this.managements = records
                    .stream()
                    .map(v -> new Entry(
                            v.getId(),
                            v.getTagId(),
                            v.getDateType(),
                            v.getCurrencyId(),
                            v.getAmount()
                    ))
                    .toList();
        }

        record Entry(long managementId, long tagId, short dateType, long currencyId, BigDecimal amount) {}
    }

    static class NewManagementResponse extends ApiResponse {
        public final long managementId;

        public NewManagementResponse(long managementId) {
            this.managementId = managementId;
        }
    }
}
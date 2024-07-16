package app.finwave.backend.api.currency;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.CurrencyConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.CurrenciesRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.ParamsValidator;

import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class CurrencyApi {
    protected CurrencyDatabase database;
    protected CurrencyConfig config;
    protected WebSocketWorker socketWorker;

    @Inject
    public CurrencyApi(DatabaseWorker databaseWorker, Configs configs, WebSocketWorker socketWorker) {
        this.database = databaseWorker.get(CurrencyDatabase.class);
        this.socketWorker = socketWorker;

        config = configs.getState(new CurrencyConfig());
    }

    public Object newCurrency(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String code = ParamsValidator
                .string(request, "code")
                .length(1, config.maxCodeLength)
                .require();

        String symbol = ParamsValidator
                .string(request, "symbol")
                .length(1, config.maxSymbolLength)
                .require();

        int decimals = ParamsValidator
                .integer(request, "decimals")
                .range(0, config.maxDecimals)
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxDescriptionLength)
                .require();

        if (database.getCurrenciesCount(sessionsRecord.getUserId()) >= config.maxCurrenciesPerUser)
            halt(409);

        Optional<Long> currencyId = database.newCurrency(sessionsRecord.getUserId(), code, symbol, (short) decimals, description);

        if (currencyId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("currencies"));

        return new NewCurrencyResponse(currencyId.get());
    }

    public Object getCurrencies(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<CurrenciesRecord> records = database.getUserCurrenciesWithRoot(sessionsRecord.getUserId());

        return new GetCurrenciesResponse(records, sessionsRecord.getUserId());
    }

    public Object editCurrencyCode(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> database.userCanEditCurrency(sessionsRecord.getUserId(), id))
                .require();

        String code = ParamsValidator
                .string(request, "code")
                .length(1, config.maxCodeLength)
                .require();

        database.editCurrencyCode(currencyId, code);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("currencies"));

        response.status(200);

        return ApiMessage.of("Currency code edited");
    }

    public Object editCurrencySymbol(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> database.userCanEditCurrency(sessionsRecord.getUserId(), id))
                .require();

        String symbol = ParamsValidator
                .string(request, "symbol")
                .length(1, config.maxCodeLength)
                .require();

        database.editCurrencySymbol(currencyId, symbol);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("currencies"));

        response.status(200);

        return ApiMessage.of("Currency symbol edited");
    }

    public Object editCurrencyDecimals(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> database.userCanEditCurrency(sessionsRecord.getUserId(), id))
                .require();

        int decimals = ParamsValidator
                .integer(request, "decimals")
                .range(1, config.maxDecimals)
                .require();

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("currencies"));

        database.editCurrencyDecimals(currencyId, (short) decimals);

        response.status(200);

        return ApiMessage.of("Currency decimals edited");
    }

    public Object editCurrencyDescription(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> database.userCanEditCurrency(sessionsRecord.getUserId(), id))
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxCodeLength)
                .require();

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("currencies"));

        database.editCurrencyDescription(currencyId, description);

        response.status(200);

        return ApiMessage.of("Currency description edited");
    }

    static class GetCurrenciesResponse extends ApiResponse {
        public final List<Entity> currencies;

        public GetCurrenciesResponse(List<CurrenciesRecord> currencies, int userId) {
            this.currencies = currencies.stream()
                    .map((r) -> new Entity(r.getId(), r.getOwnerId() == userId, r.getCode(), r.getSymbol(), r.getDecimals(), r.getDescription()))
                    .toList();
        }

        record Entity(long currencyId, boolean owned, String code, String symbol, short decimals, String description) {}
    }

    static class NewCurrencyResponse extends ApiResponse {
        public final long currencyId;

        public NewCurrencyResponse(long currencyId) {
            this.currencyId = currencyId;
        }
    }

}

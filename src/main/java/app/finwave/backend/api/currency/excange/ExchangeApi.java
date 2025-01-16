package app.finwave.backend.api.currency.excange;

import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.currency.CurrencyApi;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.CurrenciesRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.util.List;

@Singleton
public class ExchangeApi {
    protected ExchangeManager manager;
    protected CurrencyDatabase currencyDatabase;

    @Inject
    public ExchangeApi(ExchangeManager manager, DatabaseWorker databaseWorker) {
        this.manager = manager;
        this.currencyDatabase = databaseWorker.get(CurrencyDatabase.class);
    }

    public Object getExchangeRate(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        String fromCurrencyCode = ParamsValidator
                .longV(request, "fromCurrencyId")
                .map(currencyDatabase::getCurrencyCode)
                .orElseThrow(() -> new InvalidParameterException("fromCurrencyId"))
                .toLowerCase();

        String toCurrencyCode = ParamsValidator
                .longV(request, "toCurrencyId")
                .map(currencyDatabase::getCurrencyCode)
                .orElseThrow(() -> new InvalidParameterException("toCurrencyId"))
                .toLowerCase();

        BigDecimal rate = manager.getExchangeRate(fromCurrencyCode, toCurrencyCode);

        if (rate.signum() < 0) {
            response.status(403);

            return ApiMessage.of("Exchange rate is unavailable");
        }

        return new RateResponse(rate);
    }

    public static class RateResponse extends ApiResponse {
        public final BigDecimal rate;

        public RateResponse(BigDecimal rate) {
            this.rate = rate;
        }
    }
}

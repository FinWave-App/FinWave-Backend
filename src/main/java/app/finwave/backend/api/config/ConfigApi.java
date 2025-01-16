package app.finwave.backend.api.config;

import app.finwave.backend.config.general.AiConfig;
import app.finwave.backend.config.general.ExchangesConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.*;
import app.finwave.backend.config.general.UserConfig;

import java.util.UUID;

@Singleton
public class ConfigApi {
    protected String authConfigJson;
    protected String hash;

    @Inject
    public ConfigApi(Configs configs) {
        PublicConfigs publicConfigs = new PublicConfigs(
                configs.getState(new UserConfig()),
                configs.getState(new AccountsConfig()),
                configs.getState(new CurrencyConfig()),
                configs.getState(new NotesConfig()),
                configs.getState(new TransactionConfig()),
                configs.getState(new AnalyticsConfig()),
                configs.getState(new NotificationsConfig()),
                configs.getState(new AccumulationConfig()),
                configs.getState(new RecurringTransactionConfig()),
                configs.getState(new ReportConfig()),
                new AiPublic(configs.getState(new AiConfig()).enabled),
                new ExchangesPublic(configs.getState(new ExchangesConfig()).fawazahmed0Exchanges.enabled)
        );

        this.authConfigJson = ApiResponse.GSON.toJson(publicConfigs);

        String configHash = UUID.nameUUIDFromBytes(authConfigJson.getBytes()).toString();
        this.hash = ApiResponse.GSON.toJson(new ConfigsHash(configHash));
    }

    public Object getConfigs(Request request, Response response) {
        return authConfigJson;
    }

    public Object hash(Request request, Response response) {
        return hash;
    }

    record PublicConfigs(UserConfig users,
                         AccountsConfig accounts,
                         CurrencyConfig currencies,
                         NotesConfig notes,
                         TransactionConfig transactions,
                         AnalyticsConfig analytics,
                         NotificationsConfig notifications,
                         AccumulationConfig accumulation,
                         RecurringTransactionConfig recurring,
                         ReportConfig reports,
                         AiPublic ai,
                         ExchangesPublic exchanges) { }

    record AiPublic(boolean enabled) {}

    record ExchangesPublic(boolean enabled) {}

    record ConfigsHash(String hash) {
    }
}

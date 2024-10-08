package app.finwave.backend.api.config;

import app.finwave.backend.config.general.AiConfig;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.*;
import app.finwave.backend.config.general.UserConfig;

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
                new AiPublic(configs.getState(new AiConfig()).enabled)
        );

        this.authConfigJson = ApiResponse.GSON.toJson(publicConfigs);
        this.hash = Hashing.sha256().hashUnencodedChars(authConfigJson).toString().substring(0, 4);
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
                         AiPublic ai) { }

    record AiPublic(boolean enabled) {}
}

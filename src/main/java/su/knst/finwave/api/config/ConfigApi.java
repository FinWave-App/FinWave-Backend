package su.knst.finwave.api.config;

import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.*;
import su.knst.finwave.config.general.UserConfig;

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
                configs.getState(new NotificationsConfig())
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

    record PublicConfigs(UserConfig users, AccountsConfig accounts, CurrencyConfig currencies, NotesConfig notes, TransactionConfig transactions, AnalyticsConfig analytics, NotificationsConfig notifications) { }
}

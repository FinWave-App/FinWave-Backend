package su.knst.fintrack.api.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.ApiResponse;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.general.UserConfig;

@Singleton
public class ConfigApi {
    protected String authConfigJson;

    @Inject
    public ConfigApi(Configs configs) {
        this.authConfigJson = ApiResponse.GSON.toJson(configs.getState(new UserConfig()));
    }

    public Object authConfigViewer(Request request, Response response) {
        return authConfigJson;
    }
}

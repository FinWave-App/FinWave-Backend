package su.knst.fintrack.api.registration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import su.knst.fintrack.api.user.UserDatabase;
import su.knst.fintrack.api.user.UserSettingsDatabase;
import su.knst.fintrack.config.Configs;
import su.knst.fintrack.config.general.AuthConfig;
import su.knst.fintrack.http.ApiMessage;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.time.ZoneId;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class RegistrationApi {
    protected static final Logger log = LoggerFactory.getLogger(RegistrationApi.class);

    protected RegistrationDatabase database;
    protected UserDatabase userDatabase;
    protected UserSettingsDatabase userSettingsDatabase;
    protected AuthConfig config;

    @Inject
    public RegistrationApi(RegistrationDatabase database, UserDatabase userDatabase, UserSettingsDatabase userSettingsDatabase, Configs configs) {
        this.database = database;
        this.userDatabase = userDatabase;
        this.userSettingsDatabase = userSettingsDatabase;

        this.config = configs.getState(new AuthConfig());
    }

    public Object register(Request request, Response response) {
        if (!config.registration.enabled)
            halt(405);

        String login = ParamsValidator.string(request, "login")
                .length(config.minLoginLength, config.maxLoginLength)
                .matches(config.registration.loginRegexFilter)
                .require();

        String password = ParamsValidator.string(request, "password")
                .length(config.minPasswordLength, config.maxPasswordLength)
                .matches(config.registration.passwordRegexFilter)
                .require();

        String lang = ParamsValidator.string(request, "lang")
                .length(2, 16) // 16 - database limit, check base migration file
                .require();

        ZoneId timezone = ParamsValidator
                .string(request, "timezone")
                .map(ZoneId::of);

        if (userDatabase.userExists(login))
            halt(409);

        Optional<Integer> userId = database.register(login, password);

        if (userId.isEmpty())
            halt(500);

        userSettingsDatabase.initUserSettings(userId.get(), lang, timezone.getId());

        response.status(201);

        return ApiMessage.of("Successful registration");
    }
}

package su.knst.finwave.http;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.knst.finwave.api.account.AccountApi;
import su.knst.finwave.api.account.tag.AccountTagApi;
import su.knst.finwave.api.analytics.AnalyticsApi;
import su.knst.finwave.api.auth.AuthApi;
import su.knst.finwave.api.auth.AuthenticationFailException;
import su.knst.finwave.api.config.ConfigApi;
import su.knst.finwave.api.currency.CurrencyApi;
import su.knst.finwave.api.note.NoteApi;
import su.knst.finwave.api.session.SessionApi;
import su.knst.finwave.api.transaction.TransactionApi;
import su.knst.finwave.api.transaction.tag.TransactionTagApi;
import su.knst.finwave.api.user.UserApi;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.HttpConfig;
import su.knst.finwave.utils.params.InvalidParameterException;

import static spark.Spark.*;

@Singleton
public class HttpWorker {
    protected static final Logger log = LoggerFactory.getLogger(HttpWorker.class);

    protected HttpConfig config;
    protected AuthApi authApi;
    protected UserApi userApi;
    protected ConfigApi configApi;
    protected NoteApi noteApi;
    protected AccountTagApi accountTagApi;
    protected AccountApi accountApi;
    protected CurrencyApi currencyApi;
    protected TransactionApi transactionApi;
    protected TransactionTagApi transactionTagApi;
    protected AnalyticsApi analyticsApi;
    protected SessionApi sessionApi;

    @Inject
    public HttpWorker(Configs configs,
                      AuthApi authApi,
                      UserApi userApi,
                      SessionApi sessionApi,
                      ConfigApi configApi,
                      NoteApi noteApi,
                      AccountTagApi accountTagApi,
                      AccountApi accountApi,
                      CurrencyApi currencyApi,
                      TransactionApi transactionApi,
                      TransactionTagApi transactionTagApi,
                      AnalyticsApi analyticsApi) {
        this.config = configs.getState(new HttpConfig());

        this.authApi = authApi;
        this.userApi = userApi;
        this.sessionApi = sessionApi;
        this.configApi = configApi;
        this.noteApi = noteApi;
        this.accountTagApi = accountTagApi;
        this.accountApi = accountApi;
        this.currencyApi = currencyApi;
        this.transactionApi = transactionApi;
        this.transactionTagApi = transactionTagApi;
        this.analyticsApi = analyticsApi;

        setup();
        patches();
    }

    protected void patches() {
        path("/user", () -> {
            before("/*", authApi::auth);

            get("/getUsername", userApi::getUsername);
            post("/changePassword", userApi::changePassword);
            post("/logout", userApi::logout);

            path("/sessions", () -> {
                get("/getList", sessionApi::getSessions);
                post("/new", sessionApi::newSession);
                post("/delete", sessionApi::deleteSession);
            });

            path("/notes", () -> {
                get("/get", noteApi::getNote);
                get("/getList", noteApi::getNotesList);
                get("/getImportant", noteApi::getImportantNotes);
                post("/new", noteApi::newNote);
                post("/edit", noteApi::editNote);
                post("/editTime", noteApi::editNoteNotificationTime);
                post("/delete", noteApi::deleteNote);
            });

            path("/currencies", () -> {
                get("/getList", currencyApi::getCurrencies);
                post("/new", currencyApi::newCurrency);
                post("/editSymbol", currencyApi::editCurrencySymbol);
                post("/editCode", currencyApi::editCurrencyCode);
                post("/editDescription", currencyApi::editCurrencyDescription);
                post("/editDecimals", currencyApi::editCurrencyDecimals);
            });

            path("/accounts", () -> {
                path("/tags", () -> {
                    get("/getList", accountTagApi::getTags);
                    post("/new", accountTagApi::newTag);
                    post("/editName", accountTagApi::editTagName);
                    post("/editDescription", accountTagApi::editTagDescription);
                    post("/delete", accountTagApi::deleteTag);
                });

                get("/getList", accountApi::getAccounts);
                post("/new", accountApi::newAccount);
                post("/editName", accountApi::editAccountName);
                post("/editDescription", accountApi::editAccountDescription);
                post("/editTag", accountApi::editAccountTag);
                post("/hide", accountApi::hideAccount);
                post("/show", accountApi::showAccount);
            });

            path("/transactions", () -> {
                path("/tags", () -> {
                    get("/getList", transactionTagApi::getTags);
                    post("/new", transactionTagApi::newTag);
                    post("/editType", transactionTagApi::editTagType);
                    post("/editParent", transactionTagApi::editTagParent);
                    post("/editName", transactionTagApi::editTagName);
                    post("/editDescription", transactionTagApi::editTagDescription);
                });

                get("/getList", transactionApi::getTransactions);
                get("/getCount", transactionApi::getTransactionsCount);
                post("/new", transactionApi::newTransaction);
                post("/newInternal", transactionApi::newInternalTransfer);
                post("/edit", transactionApi::editTransaction);
                post("/delete", transactionApi::deleteTransaction);
            });

            path("/analytics", () -> {
                get("/getByMonths", analyticsApi::getAnalyticsByMonths);
                get("/getByDays", analyticsApi::getAnalyticsByDays);
            });
        });

        path("/auth", () -> {
            post("/login", authApi::login);
            post("/register", userApi::register);
        });

        path("/configs", () -> {
            get("/get", configApi::getConfigs);
            get("/hash", configApi::hash);
        });
    }

    protected void setup() {
        port(config.port);
        //ipAddress("0.0.0.0");

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null)
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null)
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", config.cors.allowedOrigins);
            response.header("Access-Control-Request-Method", config.cors.allowedMethods);
            response.header("Access-Control-Allow-Headers", config.cors.allowedHeaders);
            response.header("Access-Control-Allow-Credentials", "true");

            response.type("application/json");
        });

        exception(InvalidParameterException.class, (exception, request, response) -> {
            response.status(400);
            log.trace(request.url() + " - 400: ", exception);

            response.body(ApiMessage.of(exception.getMessage()).toString());
        });

        exception(IllegalArgumentException.class, (exception, request, response) -> {
            response.status(400);
            log.trace(request.url() + " - 400: ", exception);

            response.body(ApiMessage.of("Illegal arguments").toString());
        });

        exception(AuthenticationFailException.class, (exception, request, response) -> {
            response.status(401);
            log.trace(request.url() + " - 401: ", exception);

            response.body(ApiMessage.of("Authentication fail").toString());
        });

        exception(Exception.class, (exception, request, response) -> {
            response.status(500);
            log.error(request.url() + " - 500: ", exception);

            response.body(ApiMessage.of("Server error").toString());
        });
    }

}

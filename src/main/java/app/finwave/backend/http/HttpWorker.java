package app.finwave.backend.http;

import app.finwave.backend.api.event.WebSocketClient;
import app.finwave.backend.api.event.WebSocketHandler;
import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.server.ServerApi;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.finwave.backend.api.account.AccountApi;
import app.finwave.backend.api.account.tag.AccountTagApi;
import app.finwave.backend.api.accumulation.AccumulationApi;
import app.finwave.backend.api.admin.AdminApi;
import app.finwave.backend.api.analytics.AnalyticsApi;
import app.finwave.backend.api.auth.AuthApi;
import app.finwave.backend.api.auth.AuthenticationFailException;
import app.finwave.backend.api.config.ConfigApi;
import app.finwave.backend.api.currency.CurrencyApi;
import app.finwave.backend.api.note.NoteApi;
import app.finwave.backend.api.notification.NotificationApi;
import app.finwave.backend.api.report.ReportApi;
import app.finwave.backend.api.session.SessionApi;
import app.finwave.backend.api.transaction.TransactionApi;
import app.finwave.backend.api.transaction.recurring.RecurringTransactionApi;
import app.finwave.backend.api.transaction.tag.TransactionTagApi;
import app.finwave.backend.api.user.UserApi;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.HttpConfig;
import app.finwave.backend.utils.params.InvalidParameterException;

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
    protected RecurringTransactionApi recurringTransactionApi;
    protected AnalyticsApi analyticsApi;
    protected SessionApi sessionApi;
    protected AdminApi adminApi;
    protected NotificationApi notificationApi;
    protected AccumulationApi accumulationApi;
    protected ReportApi reportApi;
    protected ServerApi serverApi;

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
                      RecurringTransactionApi recurringTransactionApi,
                      AnalyticsApi analyticsApi,
                      AdminApi adminApi,
                      NotificationApi notificationApi,
                      AccumulationApi accumulationApi,
                      ReportApi reportApi,
                      ServerApi serverApi) {
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
        this.recurringTransactionApi = recurringTransactionApi;
        this.adminApi = adminApi;
        this.notificationApi = notificationApi;
        this.accumulationApi = accumulationApi;
        this.reportApi = reportApi;
        this.serverApi = serverApi;

        webSocket("/websockets/events", WebSocketHandler.class);

        setup();
        patches();
    }

    protected void patches() {
        path("/admin", () -> {
            before("/*", authApi::authAdmin);

            get("/check", (request, response) -> true);
            get("/getUsers", adminApi::getUsers);
            get("/getActiveUsersCount", adminApi::getActiveUsersCount);
            get("/getUsersCount", adminApi::getUsersCount);
            get("/getTransactionsCount", adminApi::getTransactionsCount);

            post("/registerUser", adminApi::registerUser);
            post("/changeUserPassword", adminApi::changeUserPassword);
        });

        path("/user", () -> {
            before("/*", authApi::auth);

            get("/getUsername", userApi::getUsername);
            post("/changePassword", userApi::changePassword);
            post("/logout", userApi::logout);

            path("/reports", () -> {
                get("/getList", reportApi::getList);
                post("/new", reportApi::newReport);
            });

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

            path("/accumulations", () -> {
                get("/getList", accumulationApi::getList);
                post("/set", accumulationApi::setAccumulation);
                post("/remove", accumulationApi::removeAccumulation);
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

                path("/recurring", () -> {
                    get("/getList", recurringTransactionApi::getList);
                    post("/new", recurringTransactionApi::newRecurringTransaction);
                    post("/edit", recurringTransactionApi::editRecurringTransaction);
                    post("/delete", recurringTransactionApi::deleteRecurringTransaction);
                });

                get("/getList", transactionApi::getTransactions);
                get("/getCount", transactionApi::getTransactionsCount);
                post("/new", transactionApi::newTransaction);
                post("/newInternal", transactionApi::newInternalTransfer);
                post("/newBulk", transactionApi::newBulkTransactions);
                post("/edit", transactionApi::editTransaction);
                post("/delete", transactionApi::deleteTransaction);
            });

            path("/analytics", () -> {
                get("/getByMonths", analyticsApi::getAnalyticsByMonths);
                get("/getByDays", analyticsApi::getAnalyticsByDays);
            });

            path("/notifications", () -> {
                path("/points", () -> {
                    get("/getList", notificationApi::getPoints);
                    get("/vapidKey", notificationApi::getKey);
                    post("/newWebPush", notificationApi::registerNewWebPushPoint);
                    post("/editDescription", notificationApi::editPointDescription);
                    post("/editPrimary", notificationApi::editPointPrimary);
                    post("/delete", notificationApi::deletePoint);
                });

                post("/push", notificationApi::pushNotification);
            });
        });

        path("/auth", () -> {
            post("/login", authApi::login);
            post("/register", userApi::register);
            post("/demo", userApi::demoAccount);
        });

        path("/configs", () -> {
            get("/get", configApi::getConfigs);
            get("/hash", configApi::hash);
        });

        path("/files", () -> {
            path("/reports", () -> {
                get("/get", reportApi::downloadReport);
            });
        });

        path("/server", () -> {
            get("/getVersion", serverApi::getVersion);
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

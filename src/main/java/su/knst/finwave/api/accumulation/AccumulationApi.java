package su.knst.finwave.api.accumulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import su.knst.finwave.api.ApiResponse;
import su.knst.finwave.api.account.AccountApi;
import su.knst.finwave.api.account.AccountDatabase;
import su.knst.finwave.api.account.tag.AccountTagDatabase;
import su.knst.finwave.api.accumulation.data.AccumulationData;
import su.knst.finwave.api.accumulation.data.AccumulationStep;
import su.knst.finwave.api.analytics.result.AnalyticsByMonths;
import su.knst.finwave.api.currency.CurrencyDatabase;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.api.transaction.tag.TransactionTagDatabase;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.app.AccountsConfig;
import su.knst.finwave.config.app.AccumulationConfig;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.http.ApiMessage;
import su.knst.finwave.jooq.tables.records.AccountsRecord;
import su.knst.finwave.jooq.tables.records.UsersSessionsRecord;
import su.knst.finwave.utils.params.InvalidParameterException;
import su.knst.finwave.utils.params.ParamsValidator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AccumulationApi {
    protected AccumulationConfig config;
    protected AccumulationDatabase database;
    protected AccountDatabase accountDatabase;
    protected TransactionTagDatabase transactionTagDatabase;

    @Inject
    public AccumulationApi(DatabaseWorker databaseWorker, Configs configs) {
        this.database = databaseWorker.get(AccumulationDatabase.class);
        this.accountDatabase = databaseWorker.get(AccountDatabase.class);
        this.transactionTagDatabase = databaseWorker.get(TransactionTagDatabase.class);

        this.config = configs.getState(new AccumulationConfig());
    }

    public Object setAccumulation(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        SetAccumulationArgs args = ParamsValidator.bodyObject(request, SetAccumulationArgs.class)
                .matches((r) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), r.sourceAccountId), "sourceAccountId")
                .matches((r) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), r.targetAccountId), "targetAccountId")
                .matches((r) -> accountDatabase.sameCurrencies(r.sourceAccountId, r.targetAccountId), "sourceAccountId / targetAccountId")
                .matches((r) -> transactionTagDatabase.userOwnTag(sessionsRecord.getUserId(), r.tagId), "tagId")
                .matches((r) -> r.validateSteps(config.maxStepsPerAccount), "steps")
                .require();

        database.setAccumulation(new AccumulationData(
                args.sourceAccountId,
                args.targetAccountId,
                args.tagId,
                sessionsRecord.getUserId(),
                args.steps
        ));

        response.status(200);

        return ApiMessage.of("Accumulation set");
    }

    public Object removeAccumulation(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long accountId = ParamsValidator
                .longV(request, "accountId")
                .matches((id) -> accountDatabase.userOwnAccount(sessionsRecord.getUserId(), id))
                .require();

        database.removeAccumulation(accountId);

        response.status(200);

        return ApiMessage.of("Accumulation removed");
    }

    public Object getList(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        List<AccumulationData> result = database.getAccumulation(sessionsRecord.getUserId());

        response.status(200);

        return new GetAccumulationsListResponse(result);
    }

    record SetAccumulationArgs(long sourceAccountId, long targetAccountId, long tagId, ArrayList<AccumulationStep> steps) {
        public boolean validateSteps(int maxSteps) {
            if (steps.isEmpty() || steps.size() > maxSteps)
                return false;

            return steps.stream().noneMatch((s) -> s.step() == null || s.step().signum() <= 0);
        }
    }

    static class GetAccumulationsListResponse extends ApiResponse {
        public final List<AccumulationData> accumulations;

        public GetAccumulationsListResponse(List<AccumulationData> result) {
            this.accumulations = result;
        }
    }
}

package app.finwave.backend.api.accumulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.account.AccountApi;
import app.finwave.backend.api.account.AccountDatabase;
import app.finwave.backend.api.account.tag.AccountTagDatabase;
import app.finwave.backend.api.accumulation.data.AccumulationData;
import app.finwave.backend.api.accumulation.data.AccumulationStep;
import app.finwave.backend.api.analytics.result.AnalyticsByMonths;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.api.transaction.tag.TransactionTagDatabase;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.AccountsConfig;
import app.finwave.backend.config.app.AccumulationConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.AccountsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;

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

package app.finwave.backend.api.budget;

import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.category.CategoryDatabase;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.CategoriesBudgetsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class CategoryBudgetApi {
    protected CategoryBudgetManager manager;
    protected WebSocketWorker socketWorker;
    protected CategoryDatabase categoryDatabase;
    protected CurrencyDatabase currencyDatabase;

    @Inject
    public CategoryBudgetApi(CategoryBudgetManager manager, WebSocketWorker socketWorker, DatabaseWorker databaseWorker) {
        this.manager = manager;
        this.socketWorker = socketWorker;
        this.categoryDatabase = databaseWorker.get(CategoryDatabase.class);
        this.currencyDatabase = databaseWorker.get(CurrencyDatabase.class);
    }

    public Object addBudget(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> currencyDatabase.userCanReadCurrency(sessionsRecord.getUserId(), id))
                .require();

        if (manager.budgetExists(sessionsRecord.getUserId(), categoryId, currencyId, -1))
            throw new InvalidParameterException("budget with that category and currency already exists");

        short dateType = ParamsValidator
                .integer(request, "dateType")
                .range(0, 1)
                .require()
                .shortValue();

        BigDecimal amount = ParamsValidator
                .string(request, "amount")
                .map(BigDecimal::new);

        if (amount.signum() == 0)
            throw new InvalidParameterException("amount cannot be equal to zero");

        Optional<Long> newId = manager.add(sessionsRecord.getUserId(), categoryId, currencyId, dateType, amount);

        if (newId.isEmpty())
            halt(500);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("categoryBudget"));

        response.status(201);

        return new NewBudgetResponse(newId.get());
    }

    public Object editBudget(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long budgetId = ParamsValidator
                .longV(request, "budgetId")
                .matches((id) -> manager.userOwnBudget(sessionsRecord.getUserId(), id))
                .require();

        long categoryId = ParamsValidator
                .longV(request, "categoryId")
                .matches((id) -> categoryDatabase.userOwnCategory(sessionsRecord.getUserId(), id))
                .require();

        long currencyId = ParamsValidator
                .longV(request, "currencyId")
                .matches((id) -> currencyDatabase.userCanReadCurrency(sessionsRecord.getUserId(), id))
                .require();

        if (manager.budgetExists(sessionsRecord.getUserId(), categoryId, currencyId, budgetId))
            throw new InvalidParameterException("budget with that category and currency already exists");

        short dateType = ParamsValidator
                .integer(request, "dateType")
                .range(0, 1)
                .require()
                .shortValue();

        BigDecimal amount = ParamsValidator
                .string(request, "amount")
                .map(BigDecimal::new);

        if (amount.signum() == 0)
            throw new InvalidParameterException("amount cannot be equal to zero");

        manager.update(sessionsRecord.getUserId(), budgetId, categoryId, currencyId, dateType, amount);

        socketWorker.sendToUser(sessionsRecord.getUserId(), new NotifyUpdate("categoryBudget"));

        response.status(200);

        return ApiMessage.of("Category budget edited");
    }

    public Object getSettings(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        response.status(200);

        return new GetListResponse(manager.getSettings(sessionsRecord.getUserId()));
    }

    public Object remove(Request request, Response response) {
        UsersSessionsRecord sessionsRecord = request.attribute("session");

        long budgetId = ParamsValidator
                .longV(request, "budgetId")
                .matches((id) -> manager.userOwnBudget(sessionsRecord.getUserId(), id))
                .require();

        boolean result = manager.remove(budgetId);

        if (!result)
            halt(500);

        response.status(200);

        return ApiMessage.of("Category budget removed");
    }

    static class GetListResponse extends ApiResponse {
        public final List<Entry> budgets;

        public GetListResponse(List<CategoriesBudgetsRecord> records) {
            this.budgets = records
                    .stream()
                    .map(v -> new Entry(
                            v.getId(),
                            v.getCategoryId(),
                            v.getDateType(),
                            v.getCurrencyId(),
                            v.getAmount()
                    ))
                    .toList();
        }

        record Entry(long budgetId, long categoryId, short dateType, long currencyId, BigDecimal amount) {}
    }

    static class NewBudgetResponse extends ApiResponse {
        public final long budgetId;

        public NewBudgetResponse(long budgetId) {
            this.budgetId = budgetId;
        }
    }
}
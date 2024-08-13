package app.finwave.backend.api.budget;

import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.CategoriesBudgetsRecord;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.CATEGORIES_BUDGETS;

public class CategoryBudgetDatabase extends AbstractDatabase {
    public CategoryBudgetDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> add(int userId, long categoryId, long currencyId, short dateType, BigDecimal amount) {
        return context.insertInto(CATEGORIES_BUDGETS)
                .set(CATEGORIES_BUDGETS.OWNER_ID, userId)
                .set(CATEGORIES_BUDGETS.CATEGORY_ID, categoryId)
                .set(CATEGORIES_BUDGETS.CURRENCY_ID, currencyId)
                .set(CATEGORIES_BUDGETS.DATE_TYPE, dateType)
                .set(CATEGORIES_BUDGETS.AMOUNT, amount)
                .returningResult(CATEGORIES_BUDGETS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public void update(long budgetId, long categoryId, long currencyId, short dateType, BigDecimal amount) {
        context.update(CATEGORIES_BUDGETS)
                .set(CATEGORIES_BUDGETS.CATEGORY_ID, categoryId)
                .set(CATEGORIES_BUDGETS.CURRENCY_ID, currencyId)
                .set(CATEGORIES_BUDGETS.DATE_TYPE, dateType)
                .set(CATEGORIES_BUDGETS.AMOUNT, amount)
                .where(CATEGORIES_BUDGETS.ID.eq(budgetId))
                .returning()
                .fetch();
    }

    public Optional<CategoriesBudgetsRecord> remove(long budgetId) {
        return context.deleteFrom(CATEGORIES_BUDGETS)
                .where(CATEGORIES_BUDGETS.ID.eq(budgetId))
                .returning()
                .fetchOptional();
    }

    public boolean budgetExists(int userId, long categoryId, long currencyId, long excludebudget) {
        return context.select(CATEGORIES_BUDGETS.ID)
                .from(CATEGORIES_BUDGETS)
                .where(CATEGORIES_BUDGETS.CATEGORY_ID.eq(categoryId)
                        .and(CATEGORIES_BUDGETS.CURRENCY_ID.eq(currencyId))
                        .and(CATEGORIES_BUDGETS.OWNER_ID.eq(userId)
                        .and(CATEGORIES_BUDGETS.ID.notEqual(excludebudget)))
                )
                .fetchOptional()
                .isPresent();
    }

    public List<CategoriesBudgetsRecord> getList(int userId) {
        return context.selectFrom(CATEGORIES_BUDGETS)
                .where(CATEGORIES_BUDGETS.OWNER_ID.eq(userId))
                .fetch();
    }

    public boolean userOwnBudget(int userId, long budgetId) {
        return context.select(CATEGORIES_BUDGETS.ID)
                .from(CATEGORIES_BUDGETS)
                .where(CATEGORIES_BUDGETS.OWNER_ID.eq(userId).and(CATEGORIES_BUDGETS.ID.eq(budgetId)))
                .fetchOptional()
                .isPresent();
    }
}

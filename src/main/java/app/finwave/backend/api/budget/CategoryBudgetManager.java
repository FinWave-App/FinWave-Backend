package app.finwave.backend.api.budget;

import app.finwave.backend.api.category.CategoryDatabase;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.CachingConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.CategoriesBudgetsRecord;
import app.finwave.backend.utils.CacheHandyBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
public class CategoryBudgetManager {
    protected CategoryBudgetDatabase database;
    protected CategoryDatabase categoryDatabase;

    protected CachingConfig cachingConfig;

    protected LoadingCache<Integer, List<CategoriesBudgetsRecord>> listCache;

    protected ArrayList<Consumer<Integer>> cacheInvalidationListeners = new ArrayList<>();

    @Inject
    public CategoryBudgetManager(DatabaseWorker worker, Configs configs) {
        this.database = worker.get(CategoryBudgetDatabase.class);
        this.categoryDatabase = worker.get(CategoryDatabase.class);

        this.cachingConfig = configs.getState(new CachingConfig());

        this.listCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.categoriesBudget.maxLists,
                database::getList,
                (notification) -> cacheInvalidationListeners.forEach((listener) -> listener.accept(notification.getKey()))
        );
    }

    public void addInvalidationListener(Consumer<Integer> listener) {
        cacheInvalidationListeners.add(listener);
    }

    public List<CategoriesBudgetsRecord> getSettings(int userId) {
        try {
            return listCache.get(userId);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return List.of();
    }

    public Optional<Long> add(int userId, long categoryId, long currencyId, short dateType, BigDecimal amount) {
        Optional<Long> result = database.add(userId, categoryId, currencyId, dateType, amount);

        if (result.isPresent())
            listCache.invalidate(userId);

        return result;
    }

    public void update(int userId, long budgetId, long categoryId, long currencyId, short dateType, BigDecimal amount) {
        database.update(budgetId, categoryId, currencyId, dateType, amount);

        listCache.invalidate(userId);
    }

    public boolean budgetExists(int userId, long categoryId, long currencyId, long excludeBudget) {
        var list = listCache.getIfPresent(userId);

        if (list == null)
            return database.budgetExists(userId, categoryId, currencyId, excludeBudget);

        for (var entry : list) {
            if (entry.getCategoryId() == categoryId && entry.getCurrencyId() == currencyId && entry.getId() != excludeBudget)
                return true;
        }

        return false;
    }

    public boolean userOwnBudget(int userId, long budgetId) {
        var list = listCache.getIfPresent(userId);

        if (list == null)
            return database.userOwnBudget(userId, budgetId);

        for (var entry : list) {
            if (entry.getId() == budgetId)
                return true;
        }

        return false;
    }

    public boolean remove(long budgetId) {
        Optional<CategoriesBudgetsRecord> record = database.remove(budgetId);

        if (record.isEmpty())
            return false;

        listCache.invalidate(record.get().getOwnerId());

        return true;
    }
}

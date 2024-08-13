package app.finwave.backend.api.analytics;

import app.finwave.backend.api.analytics.result.AnalyticsByDays;
import app.finwave.backend.api.analytics.result.AnalyticsByMonths;
import app.finwave.backend.api.analytics.result.CategorySummary;
import app.finwave.backend.api.analytics.result.CategorySummaryWithBudget;
import app.finwave.backend.api.category.CategoryDatabase;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.api.transaction.hook.TransactionActionsHook;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.transaction.manager.records.TransactionNewInternalRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.api.category.BudgetTree;
import app.finwave.backend.api.budget.CategoryBudgetManager;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.CachingConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.CategoriesBudgetsRecord;
import app.finwave.backend.jooq.tables.records.CategoriesRecord;
import app.finwave.backend.utils.CacheHandyBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.flywaydb.core.internal.util.Pair;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

@Singleton
public class AnalyticsManager {
    protected CachingConfig cachingConfig;
    protected AnalyticsDatabase database;
    protected CategoryDatabase categoryDatabase;

    protected CategoryBudgetManager categoryBudgetManager;

    protected LoadingCache<Pair<Integer, TransactionsFilter>, AnalyticsByDays> daysCache;
    protected LoadingCache<Pair<Integer, TransactionsFilter>, AnalyticsByMonths> monthsCache;

    protected LoadingCache<Pair<Integer, OffsetDateTime>, List<CategorySummaryWithBudget>> categoriesSummariesCache;
    protected Cache<Integer, HashSet<OffsetDateTime>> loadedCategoriesSummaries;

    protected Cache<Integer, HashSet<TransactionsFilter>> loadedDays;
    protected Cache<Integer, HashSet<TransactionsFilter>> loadedMonths;

    @Inject
    public AnalyticsManager(DatabaseWorker databaseWorker, Configs configs, TransactionsManager transactionsManager, CategoryBudgetManager categoryBudgetManager) {
        this.database = databaseWorker.get(AnalyticsDatabase.class);
        this.categoryDatabase = databaseWorker.get(CategoryDatabase.class);

        this.categoryBudgetManager = categoryBudgetManager;

        this.cachingConfig = configs.getState(new CachingConfig());

        this.loadedCategoriesSummaries = CacheHandyBuilder.cache(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxDaysEntries
        );

        this.categoriesSummariesCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxCategoriesSummingEntries,
                (p) -> {
                    try {
                        loadedCategoriesSummaries.get(p.getLeft(), HashSet::new).add(p.getRight());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    return calculateCategoriesSummary(p.getLeft(), p.getRight());
                },
                (entry) -> {
                    HashSet<OffsetDateTime> loaded = loadedCategoriesSummaries.getIfPresent(entry.getKey().getLeft());
                    if (loaded == null)
                        return;

                    loaded.remove(entry.getKey().getRight());
                }
        );

        this.loadedDays = CacheHandyBuilder.cache(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxDaysEntries
        );

        this.loadedMonths = CacheHandyBuilder.cache(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxMonthsEntries
        );

        this.daysCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxDaysEntries,
                (p) -> {
                    try {
                        loadedDays.get(p.getLeft(), HashSet::new).add(p.getRight());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    return database.getAnalyticsByDays(p.getLeft(), p.getRight());
                },
                (entry) -> {
                    HashSet<TransactionsFilter> loaded = loadedDays.getIfPresent(entry.getKey().getLeft());
                    if (loaded == null)
                        return;

                    loaded.remove(entry.getKey().getRight());
                }
        );

        this.monthsCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxMonthsEntries,
                (p) -> {
                    try {
                        loadedMonths.get(p.getLeft(), HashSet::new).add(p.getRight());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    return database.getAnalyticsByMonths(p.getLeft(), p.getRight());
                },
                (entry) -> {
                    int userId = entry.getKey().getLeft();

                    categoriesSummariesCache.invalidate(userId);

                    HashSet<TransactionsFilter> loaded = loadedMonths.getIfPresent(userId);
                    if (loaded == null)
                        return;

                    loaded.remove(entry.getKey().getRight());
                }
        );
        transactionsManager.getDefaultActionsWorker().addHook(new Hook<>(this));
        transactionsManager.getInternalActionsWorker().addHook(new Hook<>(this));
        transactionsManager.getRecurringActionsWorker().addHook(new Hook<>(this));
        transactionsManager.getAccumulationActionsWorker().addHook(new Hook<>(this));

        categoryBudgetManager.addInvalidationListener((userId) -> {
            HashSet<OffsetDateTime> loaded = loadedCategoriesSummaries.getIfPresent(userId);

            if (loaded == null)
                return;

            loadedCategoriesSummaries.invalidate(userId);
            loaded.stream()
                    .map((d) -> Pair.of(userId, d))
                    .forEach(categoriesSummariesCache::invalidate);

        });
    }

    protected Pair<OffsetDateTime, OffsetDateTime> dateTypeToRange(short type, OffsetDateTime referenceDate) {
        OffsetDateTime thisMonthStart = referenceDate
                .withDayOfMonth(1);

        OffsetDateTime thisMonthEnd = referenceDate
                .withDayOfMonth(YearMonth.from(referenceDate).lengthOfMonth())
                .plusHours(23)
                .plusMinutes(59)
                .plusSeconds(59)
                .plusNanos(999999999);

        if (type == 0)
            return Pair.of(thisMonthStart, thisMonthEnd);

        int month = referenceDate.getMonthValue();

        Month startMonth = Month.of(((month - 1) / 3) * 3 + 1);
        Month endMonth = Month.of(((month - 1) / 3 + 1) * 3);

        return Pair.of(
                thisMonthStart.withMonth(startMonth.getValue()),
                thisMonthEnd.withMonth(endMonth.getValue())
        );
    }

    protected List<CategorySummaryWithBudget> calculateCategoriesSummary(int userId, OffsetDateTime referenceDate) {
        List<CategoriesBudgetsRecord> settings = categoryBudgetManager.getSettings(userId);

        HashSet<Long> categories = new HashSet<>();
        HashSet<Long> currencies = new HashSet<>();

        short maxType = 0;

        for (CategoriesBudgetsRecord record : settings) {
            categories.add(record.getCategoryId());
            currencies.add(record.getCurrencyId());

            maxType = (short) Math.max(maxType, record.getDateType());
        }

        Pair<OffsetDateTime, OffsetDateTime> dateRange = dateTypeToRange(maxType, referenceDate);

        HashSet<Long> categoriesToRequest = new HashSet<>(categories);
        HashMap<Long, ArrayList<Long>> parentToChildren = new HashMap<>();

        List<CategoriesRecord> usersCategories = categoryDatabase.getCategories(userId);

        usersCategories.forEach(t -> {
            BudgetTree tree = BudgetTree.of(t.getParentsTree());

            for (Long categoryId : categories) {
                if (tree.contains(categoryId)) {
                    categoriesToRequest.add(t.getId());

                    ArrayList<Long> children = parentToChildren.computeIfAbsent(categoryId, k -> new ArrayList<>());

                    children.add(t.getId());

                    break;
                }
            }
        });

        TransactionsFilter filter = new TransactionsFilter(
                categoriesToRequest.stream().toList(), null,
                currencies.stream().toList(),
                dateRange.getLeft(),
                dateRange.getRight(),
                null
        );

        AnalyticsByMonths analyticsResult = getAnalyticsByMonths(userId, filter);
        List<CategorySummary> merged = analyticsResult.mergeAll();
        Map<LocalDate, List<CategorySummary>> analytics = analyticsResult.getTotal();

        ArrayList<CategorySummaryWithBudget> result = new ArrayList<>();

        LocalDate date = referenceDate.toLocalDate().withDayOfMonth(1);

        for (CategoriesBudgetsRecord record : settings) {
            BigDecimal amount = BigDecimal.ZERO;
            long currencyId = record.getCurrencyId();
            long categoryId = record.getCategoryId();
            short dateType = record.getDateType();

            List<CategorySummary> entries = Optional.ofNullable(dateType == 0 ? analytics.get(date) : merged).orElse(List.of());
            List<BigDecimal> toAdd = entries
                    .stream()
                    .filter((e) -> e.currencyId() == currencyId &&
                            (e.categoryId() == categoryId || parentToChildren.getOrDefault(categoryId, new ArrayList<>()).contains(e.categoryId())))
                    .map(CategorySummary::delta)
                    .toList();

            for (BigDecimal decimal : toAdd)
                amount = amount.add(decimal);

            result.add(new CategorySummaryWithBudget(currencyId, categoryId, record.getId(), amount));
        }

        return Collections.unmodifiableList(result);
    }

    public List<CategorySummaryWithBudget> getCategoriesAnalytics(int userId, OffsetDateTime referenceDate) {
        try {
            return categoriesSummariesCache.get(Pair.of(userId, referenceDate));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return List.of();
    }

    public AnalyticsByMonths getAnalyticsByMonths(int userId, TransactionsFilter filter) {
        try {
            return monthsCache.get(Pair.of(userId, filter));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return AnalyticsByMonths.EMPTY;
    }

    public AnalyticsByDays getAnalyticsByDays(int userId, TransactionsFilter filter) {
        try {
            return daysCache.get(Pair.of(userId, filter));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return AnalyticsByDays.EMPTY;
    }

    protected static class Hook<T, Y> implements TransactionActionsHook<T, Y> {
        protected AnalyticsManager manager;

        public Hook(AnalyticsManager manager) {
            this.manager = manager;
        }

        protected void invalidate(int userId) {
            var daysLoaded = manager.loadedDays.getIfPresent(userId);

            if (daysLoaded != null) {
                manager.daysCache.invalidateAll(
                        daysLoaded.stream()
                                .map((f) -> Pair.of(userId, f))
                                .toList()
                );

                manager.loadedDays.invalidate(userId);
            }

            var months = manager.loadedMonths.getIfPresent(userId);

            if (months != null) {
                manager.monthsCache.invalidateAll(
                        months.stream()
                                .map((f) -> Pair.of(userId, f))
                                .toList()
                );

                manager.loadedMonths.invalidate(userId);
            }

           var categories = manager.loadedCategoriesSummaries.getIfPresent(userId);

            if (categories != null) {
                manager.categoriesSummariesCache.invalidateAll(
                        categories.stream()
                        .map((d) -> Pair.of(userId, d))
                        .toList());

                manager.loadedCategoriesSummaries.invalidate(userId);
            }
        }

        @Override
        public void apply(DSLContext context, T newRecord) {

        }

        @Override
        public void edit(DSLContext context, Record record, Y editRecord, long transactionId) {

        }

        @Override
        public void cancel(DSLContext context, Record record, long transactionId) {

        }

        @Override
        public void applied(DSLContext context, T newRecord, long transactionId) {
            if (newRecord instanceof TransactionNewRecord r) {
                invalidate(r.userId());
            }else if (newRecord instanceof TransactionNewInternalRecord r) {
                invalidate(r.userId());
            }
        }

        @Override
        public void edited(DSLContext context, Record record, Y editRecord, long transactionId) {
            invalidate(record.get(TRANSACTIONS.OWNER_ID));
        }

        @Override
        public void canceled(DSLContext context, Record record, long transactionId) {
            invalidate(record.get(TRANSACTIONS.OWNER_ID));
        }
    }
}

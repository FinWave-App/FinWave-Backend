package app.finwave.backend.api.analytics;

import app.finwave.backend.api.analytics.result.AnalyticsByDays;
import app.finwave.backend.api.analytics.result.AnalyticsByMonths;
import app.finwave.backend.api.analytics.result.TagSummary;
import app.finwave.backend.api.analytics.result.TagSummaryWithManagement;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.api.transaction.hook.TransactionActionsHook;
import app.finwave.backend.api.transaction.manager.TransactionsManager;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewInternalRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.api.transaction.tag.TagTree;
import app.finwave.backend.api.transaction.tag.TransactionTagDatabase;
import app.finwave.backend.api.transaction.tag.managment.TagManagementManager;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.CachingConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.TransactionsTagsManagementRecord;
import app.finwave.backend.jooq.tables.records.TransactionsTagsRecord;
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
import java.util.stream.Collectors;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

@Singleton
public class AnalyticsManager {
    protected CachingConfig cachingConfig;
    protected AnalyticsDatabase database;
    protected TransactionTagDatabase tagDatabase;

    protected TagManagementManager tagManagementManager;

    protected LoadingCache<Pair<Integer, TransactionsFilter>, AnalyticsByDays> daysCache;
    protected LoadingCache<Pair<Integer, TransactionsFilter>, AnalyticsByMonths> monthsCache;

    protected LoadingCache<Pair<Integer, OffsetDateTime>, List<TagSummaryWithManagement>> tagSummariesCache;
    protected Cache<Integer, HashSet<OffsetDateTime>> loadedTagSummaries;

    protected Cache<Integer, HashSet<TransactionsFilter>> loadedDays;
    protected Cache<Integer, HashSet<TransactionsFilter>> loadedMonths;

    @Inject
    public AnalyticsManager(DatabaseWorker databaseWorker, Configs configs, TransactionsManager transactionsManager, TagManagementManager tagManagementManager) {
        this.database = databaseWorker.get(AnalyticsDatabase.class);
        this.tagDatabase = databaseWorker.get(TransactionTagDatabase.class);

        this.tagManagementManager = tagManagementManager;

        this.cachingConfig = configs.getState(new CachingConfig());

        this.loadedTagSummaries = CacheHandyBuilder.cache(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxDaysEntries
        );

        this.tagSummariesCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.analytics.maxTagSummingEntries,
                (p) -> {
                    try {
                        loadedTagSummaries.get(p.getLeft(), HashSet::new).add(p.getRight());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    return calculateTagSummary(p.getLeft(), p.getRight());
                },
                (entry) -> {
                    HashSet<OffsetDateTime> loaded = loadedTagSummaries.getIfPresent(entry.getKey().getLeft());
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

                    tagSummariesCache.invalidate(userId);

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

        tagManagementManager.addInvalidationListener((userId) -> {
            HashSet<OffsetDateTime> loaded = loadedTagSummaries.getIfPresent(userId);

            if (loaded == null)
                return;

            loadedTagSummaries.invalidate(userId);
            loaded.stream()
                    .map((d) -> Pair.of(userId, d))
                    .forEach(tagSummariesCache::invalidate);

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

    protected List<TagSummaryWithManagement> calculateTagSummary(int userId, OffsetDateTime referenceDate) {
        List<TransactionsTagsManagementRecord> settings = tagManagementManager.getSettings(userId);

        HashSet<Long> tags = new HashSet<>();
        HashSet<Long> currencies = new HashSet<>();

        short maxType = 0;

        for (TransactionsTagsManagementRecord record : settings) {
            tags.add(record.getTagId());
            currencies.add(record.getCurrencyId());

            maxType = (short) Math.max(maxType, record.getDateType());
        }

        Pair<OffsetDateTime, OffsetDateTime> dateRange = dateTypeToRange(maxType, referenceDate);

        HashSet<Long> tagsToRequest = new HashSet<>(tags);
        HashMap<Long, ArrayList<Long>> parentToChildren = new HashMap<>();

        List<TransactionsTagsRecord> userTags = tagDatabase.getTags(userId);

        userTags.forEach(t -> {
            TagTree tree = TagTree.of(t.getParentsTree());

            for (Long tagId : tags) {
                if (tree.contains(tagId)) {
                    tagsToRequest.add(t.getId());

                    ArrayList<Long> children = parentToChildren.computeIfAbsent(tagId, k -> new ArrayList<>());

                    children.add(t.getId());

                    break;
                }
            }
        });

        TransactionsFilter filter = new TransactionsFilter(
                tagsToRequest.stream().toList(), null,
                currencies.stream().toList(),
                dateRange.getLeft(),
                dateRange.getRight(),
                null
        );

        AnalyticsByMonths analyticsResult = getAnalyticsByMonths(userId, filter);
        List<TagSummary> merged = analyticsResult.mergeAll();
        Map<LocalDate, List<TagSummary>> analytics = analyticsResult.getTotal();

        ArrayList<TagSummaryWithManagement> result = new ArrayList<>();

        LocalDate date = referenceDate.toLocalDate().withDayOfMonth(1);

        for (TransactionsTagsManagementRecord record : settings) {
            BigDecimal amount = BigDecimal.ZERO;
            long currencyId = record.getCurrencyId();
            long tagId = record.getTagId();
            short dateType = record.getDateType();

            List<TagSummary> entries = Optional.ofNullable(dateType == 0 ? analytics.get(date) : merged).orElse(List.of());
            List<BigDecimal> toAdd = entries
                    .stream()
                    .filter((e) -> e.currencyId() == currencyId &&
                            (e.tagId() == tagId || parentToChildren.getOrDefault(tagId, new ArrayList<>()).contains(e.tagId())))
                    .map(TagSummary::delta)
                    .toList();

            for (BigDecimal decimal : toAdd)
                amount = amount.add(decimal);

            result.add(new TagSummaryWithManagement(currencyId, tagId, record.getId(), amount));
        }

        return Collections.unmodifiableList(result);
    }

    public List<TagSummaryWithManagement> getTagsAnalytics(int userId, OffsetDateTime referenceDate) {
        try {
            return tagSummariesCache.get(Pair.of(userId, referenceDate));
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

           var tags = manager.loadedTagSummaries.getIfPresent(userId);

            if (tags != null) {
                manager.tagSummariesCache.invalidateAll(
                        tags.stream()
                        .map((d) -> Pair.of(userId, d))
                        .toList());

                manager.loadedTagSummaries.invalidate(userId);
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

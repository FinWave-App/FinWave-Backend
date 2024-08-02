package app.finwave.backend.api.transaction.tag.managment;

import app.finwave.backend.api.analytics.AnalyticsManager;
import app.finwave.backend.api.analytics.result.AnalyticsByMonths;
import app.finwave.backend.api.analytics.result.TagSummary;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.api.transaction.tag.TagTree;
import app.finwave.backend.api.transaction.tag.TransactionTagDatabase;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.CachingConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.TransactionsTagsManagementRecord;
import app.finwave.backend.jooq.tables.records.TransactionsTagsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.CacheHandyBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.checkerframework.checker.units.qual.A;
import org.flywaydb.core.internal.util.Pair;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Singleton
public class TagManagementManager {
    protected TagManagementDatabase database;
    protected TransactionTagDatabase tagDatabase;

    protected CachingConfig cachingConfig;

    protected LoadingCache<Integer, List<TransactionsTagsManagementRecord>> listCache;

    protected ArrayList<Consumer<Integer>> cacheInvalidationListeners = new ArrayList<>();

    @Inject
    public TagManagementManager(DatabaseWorker worker, Configs configs) {
        this.database = worker.get(TagManagementDatabase.class);
        this.tagDatabase = worker.get(TransactionTagDatabase.class);

        this.cachingConfig = configs.getState(new CachingConfig());

        this.listCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.tagsManagement.maxLists,
                database::getList,
                (notification) -> cacheInvalidationListeners.forEach((listener) -> listener.accept(notification.getKey()))
        );
    }

    public void addInvalidationListener(Consumer<Integer> listener) {
        cacheInvalidationListeners.add(listener);
    }

    public List<TransactionsTagsManagementRecord> getSettings(int userId) {
        try {
            return listCache.get(userId);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return List.of();
    }

    public Optional<Long> add(int userId, long tagId, long currencyId, short dateType, BigDecimal amount) {
        Optional<Long> result = database.add(userId, tagId, currencyId, dateType, amount);

        if (result.isPresent())
            listCache.invalidate(userId);

        return result;
    }

    public void update(int userId, long managementId, long tagId, long currencyId, short dateType, BigDecimal amount) {
        database.update(managementId, tagId, currencyId, dateType, amount);

        listCache.invalidate(userId);
    }

    public boolean managementExists(int userId, long tagId, long currencyId, long excludeManagement) {
        var list = listCache.getIfPresent(userId);

        if (list == null)
            return database.managementExists(userId, tagId, currencyId, excludeManagement);

        for (var entry : list) {
            if (entry.getTagId() == tagId && entry.getCurrencyId() == currencyId && entry.getId() != excludeManagement)
                return true;
        }

        return false;
    }

    public boolean userOwnManagement(int userId, long managementId) {
        var list = listCache.getIfPresent(userId);

        if (list == null)
            return database.userOwnManagement(userId, managementId);

        for (var entry : list) {
            if (entry.getId() == managementId)
                return true;
        }

        return false;
    }

    public boolean remove(long managementId) {
        Optional<TransactionsTagsManagementRecord> record = database.remove(managementId);

        if (record.isEmpty())
            return false;

        listCache.invalidate(record.get().getOwnerId());

        return true;
    }
}

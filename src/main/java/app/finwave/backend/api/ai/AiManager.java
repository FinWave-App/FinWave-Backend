package app.finwave.backend.api.ai;

import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.CachingConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.AiMessagesRecord;
import app.finwave.backend.utils.CacheHandyBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.stefanbratanov.jvm.openai.Usage;
import org.flywaydb.core.internal.util.Pair;
import org.jooq.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class AiManager {
    protected AiDatabase database;
    protected CachingConfig.Ai cachingConfig;

    protected LoadingCache<Long, ArrayList<AiMessagesRecord>> contextMessagesCache;
    protected LoadingCache<Pair<Integer, Long>, Boolean> userOwnContextCache;

    @Inject
    public AiManager(DatabaseWorker worker, Configs configs) {
        this.database = worker.get(AiDatabase.class);
        this.cachingConfig = configs.getState(new CachingConfig()).ai;

        this.contextMessagesCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.maxContexts,
                (contextId) -> new ArrayList<>(database.getMessages(contextId))
        );

        this.userOwnContextCache = CacheHandyBuilder.loading(
                1, TimeUnit.DAYS,
                cachingConfig.maxContexts,
                (p) -> database.userOwnContext(p.getLeft(), p.getRight())
        );
    }

    public Optional<Long> newContext(int userId) {
        Optional<Long> result = database.newContext(userId);

        if (result.isPresent())
            userOwnContextCache.put(Pair.of(userId, result.get()), true);

        return result;
    }

    public boolean userOwnContext(int userId, long contextId) {
        try {
            return userOwnContextCache.get(Pair.of(userId, contextId));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean pushMessage(long contextId, String role, JSON content, Usage usage) {
        Optional<AiMessagesRecord> pushed = database.pushMessage(contextId, role, content);

        if (pushed.isEmpty())
            return false;

        if (usage != null)
            database.addUsedTokens(contextId, usage.completionTokens(), usage.promptTokens());

        ArrayList<AiMessagesRecord> cacheRecords = contextMessagesCache.getIfPresent(contextId);

        if (cacheRecords != null)
            cacheRecords.add(pushed.get());

        return true;
    }

    public List<AiMessagesRecord> getMessages(long contextId) {
        try {
            return contextMessagesCache.get(contextId);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return List.of();
    }
}

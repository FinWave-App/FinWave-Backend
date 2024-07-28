package app.finwave.backend.utils;

import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CacheHandyBuilder {
    public static CacheBuilder<Object, Object> genericBuilder(long duration, TimeUnit unit, long maxSize) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

        if (duration > 0 && unit != null)
            builder = builder.expireAfterAccess(duration, unit);

        if (maxSize > 0)
            builder = builder.maximumSize(maxSize);

        return builder;
    }

    public static <T, X> LoadingCache<T, X> loading(long duration, TimeUnit unit, long maxSize, Function<T, X> loader) {
        CacheBuilder<Object, Object> builder = genericBuilder(duration, unit, maxSize);

        return builder.build(new CacheLoader<>() {
            @Override
            public X load(T key) {
                return loader.apply(key);
            }
        });
    }

    public <T, X> LoadingCache<T, X> loading(long duration, TimeUnit unit, Function<T, X> loader) {
        return loading(duration, unit, 0, loader);
    }

    public <T, X> LoadingCache<T, X> loading(Function<T, X> loader) {
        return loading(0, null, loader);
    }

    public static <T, X> Cache<T, X> cache(long duration, TimeUnit unit, long maxSize) {
        CacheBuilder<Object, Object> builder = genericBuilder(duration, unit, maxSize);

        return builder.build();
    }

    public static <T, X> Cache<T, X> cache(long duration, TimeUnit unit) {
        return cache(duration, unit, 0);
    }

    public static <T, X> Cache<T, X> cache() {
        return cache(0, null);
    }
}

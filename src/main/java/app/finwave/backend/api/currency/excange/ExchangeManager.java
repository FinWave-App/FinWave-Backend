package app.finwave.backend.api.currency.excange;

import app.finwave.backend.api.ai.tools.AiTools;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.CurrencyConfig;
import app.finwave.backend.config.general.ExchangesConfig;
import app.finwave.backend.utils.CacheHandyBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.internal.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class ExchangeManager {
    protected Cache<Pair<String, String>, BigDecimal> exchangeRateCache;
    protected ExchangesConfig.Fawazahmed0Exchanges config;
    protected Gson gson = new Gson();

    protected static final Logger log = LoggerFactory.getLogger(ExchangeManager.class);

    @Inject
    public ExchangeManager(Configs configs) {
        this.config = configs.getState(new ExchangesConfig()).fawazahmed0Exchanges;

        this.exchangeRateCache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(config.hoursCaching, TimeUnit.HOURS)
                .build();
    }

    public BigDecimal getExchangeRate(String fromCode, String toCode) {
        if (!config.enabled)
            return BigDecimal.valueOf(-1);

        BigDecimal rate = exchangeRateCache.getIfPresent(Pair.of(fromCode, toCode));

        if (rate != null)
            return rate;

        Map<String, BigDecimal> result = fawazahmed0Fetch(fromCode, 0);

        if (result == null)
            return BigDecimal.valueOf(-1);

        Map<Pair<String, String>, BigDecimal> toLoad = result.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Pair.of(fromCode, e.getKey()),
                        Map.Entry::getValue
                ));

        exchangeRateCache.putAll(toLoad);

        return result.getOrDefault(toCode, BigDecimal.valueOf(-1));
    }

    protected Map<String, BigDecimal> fawazahmed0Fetch(String currencyCode, int serverIndex) {
        if (serverIndex >= config.servers.length)
            return null;

        String server = config.servers[serverIndex];

        try {
            String rawOutput = IOUtils.toString(new URL(server + currencyCode + ".min.json"), StandardCharsets.UTF_8);
            JsonElement element = gson.fromJson(rawOutput, JsonElement.class);

            Type mapType = new TypeToken<Map<String, BigDecimal>>() {}.getType();

            return gson.fromJson(element.getAsJsonObject().get(currencyCode), mapType);
        } catch (IOException | JsonSyntaxException e) {
            log.warn("Failed to fetch currencies rates from {}", server, e);

            return fawazahmed0Fetch(currencyCode, serverIndex + 1);
        }
    }
}

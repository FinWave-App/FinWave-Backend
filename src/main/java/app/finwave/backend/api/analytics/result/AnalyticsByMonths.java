package app.finwave.backend.api.analytics.result;

import com.google.common.collect.Maps;
import org.flywaydb.core.internal.util.Pair;
import org.jooq.Record5;
import org.jooq.Result;
import app.finwave.backend.api.ApiResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AnalyticsByMonths extends ApiResponse {
    protected Map<LocalDate, List<TagSummary>> total;

    public static final AnalyticsByMonths EMPTY = new AnalyticsByMonths(List.of());

    public AnalyticsByMonths(List<Record5<Long, Long, Integer, Integer, BigDecimal>> result) {
        HashMap<LocalDate, ArrayList<TagSummary>> tempMap = new HashMap<>();

        result.forEach((r) -> {
            LocalDate date = LocalDate.of(r.component4(), r.component3(), 1);

            if (!tempMap.containsKey(date))
                tempMap.put(date, new ArrayList<>());

            tempMap.get(date)
                    .add(new TagSummary(r.component1(), r.component2(), r.component5()));
        });

        HashMap<LocalDate, List<TagSummary>> resultMap = new HashMap<>();
        tempMap.forEach((k, v) -> resultMap.put(k, Collections.unmodifiableList(v)));

        this.total = Collections.unmodifiableMap(resultMap);
    }

    public Map<LocalDate, List<TagSummary>> getTotal() {
        return total;
    }

    public List<TagSummary> mergeAll() {
        HashMap<Pair<Long, Long>, BigDecimal> mergedMap = new HashMap<>();

        total.forEach((k, v) -> v.forEach((e) -> {
            Pair<Long, Long> pair = Pair.of(e.currencyId(), e.tagId());

            mergedMap.put(pair, mergedMap.getOrDefault(pair, BigDecimal.ZERO).add(e.delta()));
        }));

        return mergedMap.entrySet().stream()
                .map((e) -> new TagSummary(e.getKey().getLeft(), e.getKey().getRight(), e.getValue()))
                .toList();
    }
}

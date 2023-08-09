package su.knst.fintrack.api.analytics.result;

import org.jooq.Record4;
import org.jooq.Record5;
import org.jooq.Result;
import su.knst.fintrack.api.ApiResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class AnalyticsByMonths extends ApiResponse {
    protected HashMap<LocalDate, ArrayList<Entry>> total = new HashMap<>();

    public AnalyticsByMonths(Result<Record5<Long, Long, Integer, Integer, BigDecimal>> result) {
        result.forEach((r) -> {
            LocalDate date = LocalDate.of(r.component4(), r.component3(), 1);

            if (!total.containsKey(date))
                total.put(date, new ArrayList<>());

            total.get(date)
                    .add(new Entry(r.component1(), r.component2(), r.component5()));
        });
    }

    protected record Entry(long currencyId, long tagId, BigDecimal delta) {}
}

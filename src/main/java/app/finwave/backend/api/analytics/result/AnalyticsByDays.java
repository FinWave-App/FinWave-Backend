package app.finwave.backend.api.analytics.result;

import org.jooq.Record6;
import org.jooq.Result;
import app.finwave.backend.api.ApiResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

public class AnalyticsByDays extends ApiResponse {
    protected HashMap<LocalDate, ArrayList<Entry>> total = new HashMap<>();

    public AnalyticsByDays(Result<Record6<Long, Long, Integer, Integer, Integer, BigDecimal>> result) {
        result.forEach((r) -> {
            LocalDate date = LocalDate.of(r.component5(), r.component4(), r.component3());

            if (!total.containsKey(date))
                total.put(date, new ArrayList<>());

            total.get(date)
                    .add(new Entry(r.component1(), r.component2(), r.component6()));
        });
    }

    protected record Entry(long currencyId, long tagId, BigDecimal delta) {}
}

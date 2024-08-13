package app.finwave.backend.api.accumulation.data;

import com.google.gson.reflect.TypeToken;
import app.finwave.backend.jooq.tables.records.AccumulationSettingsRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static app.finwave.backend.api.ApiResponse.GSON;

public record AccumulationData(long sourceAccountId, long targetAccountId, long categoryId, int ownerId, ArrayList<AccumulationStep> steps) {
    public static AccumulationData fromRecord(AccumulationSettingsRecord record) {
        return new AccumulationData(
                record.getSourceAccountId(),
                record.getTargetAccountId(),
                record.getCategoryId(),
                record.getOwnerId(),
                GSON.fromJson(record.getSteps().data(), new TypeToken<List<AccumulationStep>>(){}.getType())
        );
    }

    public BigDecimal calculateRound(BigDecimal delta) {
        for (AccumulationStep step : steps()) {
            BigDecimal round = step.calculateRound(delta);

            if (!round.equals(BigDecimal.ZERO))
                return round;
        }

        return BigDecimal.ZERO;
    }
}

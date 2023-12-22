package su.knst.finwave.api.accumulation.data;

import com.google.gson.reflect.TypeToken;
import su.knst.finwave.jooq.tables.records.AccumulationSettingsRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static su.knst.finwave.api.notification.NotificationDatabase.GSON;

public record AccumulationData(long sourceAccountId, long targetAccountId, long tagId, int ownerId, ArrayList<AccumulationStep> steps) {
    public static AccumulationData fromRecord(AccumulationSettingsRecord record) {
        return new AccumulationData(
                record.getSourceAccountId(),
                record.getTargetAccountId(),
                record.getTagId(),
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

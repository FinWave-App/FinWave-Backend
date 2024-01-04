package su.knst.finwave.api.accumulation;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import su.knst.finwave.api.accumulation.data.AccumulationData;
import su.knst.finwave.database.AbstractDatabase;

import java.util.List;
import java.util.Optional;

import static su.knst.finwave.api.ApiResponse.GSON;
import static su.knst.finwave.jooq.Tables.ACCUMULATION_SETTINGS;

public class AccumulationDatabase extends AbstractDatabase {
    public AccumulationDatabase(DSLContext context) {
        super(context);
    }

    public void setAccumulation(AccumulationData accumulation) {
        context.insertInto(ACCUMULATION_SETTINGS)
                .set(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID, accumulation.sourceAccountId())
                .set(ACCUMULATION_SETTINGS.TARGET_ACCOUNT_ID, accumulation.targetAccountId())
                .set(ACCUMULATION_SETTINGS.TAG_ID, accumulation.tagId())
                .set(ACCUMULATION_SETTINGS.OWNER_ID, accumulation.ownerId())
                .set(ACCUMULATION_SETTINGS.STEPS, JSONB.valueOf(GSON.toJson(accumulation.steps())))
                .onConflict(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID)
                .doUpdate()
                .set(ACCUMULATION_SETTINGS.TARGET_ACCOUNT_ID, accumulation.targetAccountId())
                .set(ACCUMULATION_SETTINGS.TAG_ID, accumulation.tagId())
                .set(ACCUMULATION_SETTINGS.STEPS, JSONB.valueOf(GSON.toJson(accumulation.steps())))
                .where(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID.eq(accumulation.sourceAccountId())
                        .and(ACCUMULATION_SETTINGS.OWNER_ID.eq(accumulation.ownerId())))
                .execute();
    }

    public void removeAccumulation(long sourceAccountId) {
        context.deleteFrom(ACCUMULATION_SETTINGS)
                .where(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID.eq(sourceAccountId))
                .execute();
    }

    public List<AccumulationData> getAccumulation(int ownerId) {
        return context.selectFrom(ACCUMULATION_SETTINGS)
                .where(ACCUMULATION_SETTINGS.OWNER_ID.eq(ownerId))
                .fetch()
                .map(AccumulationData::fromRecord);
    }

    public Optional<AccumulationData> getAccumulationSettings(long sourceAccountId) {
        return context.selectFrom(ACCUMULATION_SETTINGS)
                .where(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID.eq(sourceAccountId))
                .fetchOptional()
                .map(AccumulationData::fromRecord);
    }

}

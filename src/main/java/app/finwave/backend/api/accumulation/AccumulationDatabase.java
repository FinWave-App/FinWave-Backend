package app.finwave.backend.api.accumulation;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import app.finwave.backend.api.accumulation.data.AccumulationData;
import app.finwave.backend.database.AbstractDatabase;
import org.jooq.Record1;

import java.util.List;
import java.util.Optional;

import static app.finwave.backend.api.ApiResponse.GSON;
import static app.finwave.backend.jooq.Tables.ACCUMULATION_SETTINGS;

public class AccumulationDatabase extends AbstractDatabase {
    public AccumulationDatabase(DSLContext context) {
        super(context);
    }

    public void setAccumulation(AccumulationData accumulation) {
        context.insertInto(ACCUMULATION_SETTINGS)
                .set(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID, accumulation.sourceAccountId())
                .set(ACCUMULATION_SETTINGS.TARGET_ACCOUNT_ID, accumulation.targetAccountId())
                .set(ACCUMULATION_SETTINGS.CATEGORY_ID, accumulation.categoryId())
                .set(ACCUMULATION_SETTINGS.OWNER_ID, accumulation.ownerId())
                .set(ACCUMULATION_SETTINGS.STEPS, JSONB.valueOf(GSON.toJson(accumulation.steps())))
                .onConflict(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID)
                .doUpdate()
                .set(ACCUMULATION_SETTINGS.TARGET_ACCOUNT_ID, accumulation.targetAccountId())
                .set(ACCUMULATION_SETTINGS.CATEGORY_ID, accumulation.categoryId())
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

    public boolean accountAffected(long accountId) {
        return context.selectCount()
                .from(ACCUMULATION_SETTINGS)
                .where(ACCUMULATION_SETTINGS.SOURCE_ACCOUNT_ID.eq(accountId).or(ACCUMULATION_SETTINGS.TARGET_ACCOUNT_ID.eq(accountId)))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0) > 0;
    }

}

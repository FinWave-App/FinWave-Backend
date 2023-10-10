package su.knst.finwave.api.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record1;
import su.knst.finwave.database.AbstractDatabase;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.UsersSettingsRecord;

import java.time.ZoneId;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.USERS_SETTINGS;

public class UserSettingsDatabase extends AbstractDatabase {

    public UserSettingsDatabase(DSLContext context) {
        super(context);
    }

    public void initUserSettings(int userId, String lang, String timezone) {
        context.insertInto(USERS_SETTINGS)
                .set(USERS_SETTINGS.USER_ID, userId)
                .set(USERS_SETTINGS.LANGUAGE, lang)
                .set(USERS_SETTINGS.TIME_ZONE, timezone)
                .execute();
    }

    public Optional<UsersSettingsRecord> getUserSettings(int userId) {
        return context.selectFrom(USERS_SETTINGS)
                .where(USERS_SETTINGS.USER_ID.eq(userId))
                .fetchOptional();
    }

    public ZoneId getUserTimezone(int userId) {
        return context.select(USERS_SETTINGS.TIME_ZONE)
                .where(USERS_SETTINGS.USER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .map(ZoneId::of)
                .orElse(ZoneId.systemDefault());
    }

    public void updateUserSettings(int userId, String lang, String timezone) {
        context.update(USERS_SETTINGS)
                .set(USERS_SETTINGS.LANGUAGE, lang)
                .set(USERS_SETTINGS.TIME_ZONE, timezone)
                .where(USERS_SETTINGS.USER_ID.eq(userId))
                .execute();
    }

    public void updateUserLangSetting(int userId, String lang) {
        context.update(USERS_SETTINGS)
                .set(USERS_SETTINGS.LANGUAGE, lang)
                .where(USERS_SETTINGS.USER_ID.eq(userId))
                .execute();
    }

    public void updateUserTimezoneSetting(int userId, String timezone) {
        context.update(USERS_SETTINGS)
                .set(USERS_SETTINGS.TIME_ZONE, timezone)
                .where(USERS_SETTINGS.USER_ID.eq(userId))
                .execute();
    }
}

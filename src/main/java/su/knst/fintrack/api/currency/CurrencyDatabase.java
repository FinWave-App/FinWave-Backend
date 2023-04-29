package su.knst.fintrack.api.currency;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record1;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.jooq.tables.records.CurrenciesRecord;

import java.util.List;
import java.util.Optional;

import static su.knst.fintrack.jooq.Tables.CURRENCIES;

@Singleton
public class CurrencyDatabase {
    protected DSLContext context;

    @Inject
    public CurrencyDatabase(Database database) {
        this.context = database.context();
    }

    public Optional<Long> newCurrency(int ownerId, String code, String symbol, String description) {
        return context.insertInto(CURRENCIES)
                .set(CURRENCIES.OWNER_ID, ownerId)
                .set(CURRENCIES.CODE, code)
                .set(CURRENCIES.SYMBOL, symbol)
                .set(CURRENCIES.DESCRIPTION, description)
                .returningResult(CURRENCIES.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public void editCurrencyCode(long currencyId, String code) {
        context.update(CURRENCIES)
                .set(CURRENCIES.CODE, code)
                .where(CURRENCIES.ID.eq(currencyId))
                .execute();
    }

    public void editCurrencySymbol(long currencyId, String symbol) {
        context.update(CURRENCIES)
                .set(CURRENCIES.SYMBOL, symbol)
                .where(CURRENCIES.ID.eq(currencyId))
                .execute();
    }

    public void editCurrencyDescription(long currencyId, String description) {
        context.update(CURRENCIES)
                .set(CURRENCIES.DESCRIPTION, description)
                .where(CURRENCIES.ID.eq(currencyId))
                .execute();
    }

    public List<CurrenciesRecord> getUserCurrenciesWithRoot(int userId) {
        return context
                .selectFrom(CURRENCIES)
                .where(CURRENCIES.OWNER_ID.eq(userId).and(CURRENCIES.OWNER_ID.eq(1)))
                .fetch();
    }

    public int getCurrenciesCount(int userId) {
        return context.selectCount()
                .from(CURRENCIES)
                .where(CURRENCIES.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public boolean userCanReadCurrency(int userId, long currencyId) {
        return context.select(CURRENCIES.OWNER_ID)
                .from(CURRENCIES)
                .where(CURRENCIES.ID.eq(currencyId))
                .fetchOptional()
                .map(Record1::component1)
                .map((c) -> c == userId || c == 1)
                .orElse(false);
    }

    public boolean userCanEditCurrency(int userId, long currencyId) {
        return context.select(CURRENCIES.OWNER_ID)
                .from(CURRENCIES)
                .where(CURRENCIES.ID.eq(currencyId).and(CURRENCIES.OWNER_ID.eq(userId)))
                .fetchOptional()
                .isPresent();
    }
}

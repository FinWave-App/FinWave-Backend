package app.finwave.backend.api.currency;

import org.jooq.DSLContext;
import org.jooq.Record1;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.CurrenciesRecord;

import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.CURRENCIES;


public class CurrencyDatabase extends AbstractDatabase {

    public CurrencyDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newCurrency(int ownerId, String code, String symbol, short decimals, String description) {
        return context.insertInto(CURRENCIES)
                .set(CURRENCIES.OWNER_ID, ownerId)
                .set(CURRENCIES.CODE, code)
                .set(CURRENCIES.SYMBOL, symbol)
                .set(CURRENCIES.DECIMALS, decimals)
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

    public void editCurrencyDecimals(long currencyId, short decimals) {
        context.update(CURRENCIES)
                .set(CURRENCIES.DECIMALS, decimals)
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
                .where(CURRENCIES.OWNER_ID.eq(userId).or(CURRENCIES.OWNER_ID.eq(1)))
                .orderBy(CURRENCIES.ID)
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

    public Optional<String> getCurrencyCode(long currencyId) {
        return context.select(CURRENCIES.CODE)
                .from(CURRENCIES)
                .where(CURRENCIES.ID.eq(currencyId))
                .fetchOptional()
                .map(Record1::component1);
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

package app.finwave.backend.api.account;

import org.jooq.DSLContext;
import org.jooq.Record1;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.AccountsRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class AccountDatabase extends AbstractDatabase {

    public AccountDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newAccount(int userId, long folderId, long currencyId, String name, String description) {
        return context.insertInto(ACCOUNTS)
                .set(ACCOUNTS.OWNER_ID, userId)
                .set(ACCOUNTS.FOLDER_ID, folderId)
                .set(ACCOUNTS.CURRENCY_ID, currencyId)
                .set(ACCOUNTS.AMOUNT, BigDecimal.ZERO)
                .set(ACCOUNTS.HIDDEN, false)
                .set(ACCOUNTS.NAME, name)
                .set(ACCOUNTS.DESCRIPTION, description)
                .returningResult(ACCOUNTS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    protected void setHiddenState(long accountId, boolean hidden) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.HIDDEN, hidden)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }

    public void hideAccount(long accountId) {
        setHiddenState(accountId, true);
    }

    public void showAccount(long accountId) {
        setHiddenState(accountId, false);
    }

    public int getAccountsCount(int userId) {
        return context.selectCount()
                .from(ACCOUNTS)
                .where(ACCOUNTS.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public boolean userOwnAccount(int userId, long accountId) {
        return context.select(ACCOUNTS.ID)
                .from(ACCOUNTS)
                .where(ACCOUNTS.OWNER_ID.eq(userId).and(ACCOUNTS.ID.eq(accountId)))
                .fetchOptional()
                .isPresent();
    }

    public boolean sameCurrencies(long id, long id2) {
        List<Long> results = context.select(ACCOUNTS.CURRENCY_ID)
                .from(ACCOUNTS)
                .where(ACCOUNTS.ID.eq(id).or(ACCOUNTS.ID.eq(id2)))
                .fetch().map(Record1::component1);

        if (results.size() != 2)
            return false;

        return results.get(0).equals(results.get(1));
    }

    public List<AccountsRecord> getAccounts(int userId) {
        return context.selectFrom(ACCOUNTS)
                .where(ACCOUNTS.OWNER_ID.eq(userId))
                .orderBy(ACCOUNTS.ID)
                .fetch();
    }

    public void editAccountFolder(long accountId, long folderId) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.FOLDER_ID, folderId)
                .where(ACCOUNTS.ID.eq((accountId)))
                .execute();
    }

    public void editAccountName(long accountId, String name) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.NAME, name)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }

    public void editAccountDescription(long accountId, String description) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.DESCRIPTION, description)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }

    public void deleteAccount(long accountId) {
        context.delete(ACCOUNTS)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }

    public void deltaAccountAmount(long accountId, BigDecimal delta) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }
}

package su.knst.fintrack.api.account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record1;
import su.knst.fintrack.database.Database;
import su.knst.fintrack.jooq.tables.Accounts;
import su.knst.fintrack.jooq.tables.records.AccountsRecord;
import su.knst.fintrack.jooq.tables.records.NotesRecord;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.fintrack.jooq.Tables.*;
import static su.knst.fintrack.jooq.Tables.NOTES;

@Singleton
public class AccountDatabase {
    protected DSLContext context;

    @Inject
    public AccountDatabase(Database database) {
        this.context = database.context();
    }

    public Optional<Long> newAccount(int userId, long tagId, long currencyId, String name, String description) {
        return context.insertInto(ACCOUNTS)
                .set(ACCOUNTS.OWNER_ID, userId)
                .set(ACCOUNTS.TAG_ID, tagId)
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

    public List<AccountsRecord> getAccounts(int userId) {
        return context.selectFrom(ACCOUNTS)
                .where(ACCOUNTS.OWNER_ID.eq(userId))
                .fetch();
    }

    public void editAccountTag(long accountId, long tagId) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.TAG_ID, tagId)
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

    public void deltaAccountAmount(long accountId, BigDecimal delta) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }
}

package app.finwave.backend.report.builders;

import com.google.gson.reflect.TypeToken;
import org.jooq.JSONB;
import org.jooq.Record;
import app.finwave.backend.api.account.AccountDatabase;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.api.transaction.tag.TransactionTagDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.AccountsRecord;
import app.finwave.backend.jooq.tables.records.CurrenciesRecord;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.jooq.tables.records.TransactionsTagsRecord;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static app.finwave.backend.api.ApiResponse.GSON;

public abstract class AbstractReportBuilder {
    protected ReportsRecord reportsRecord;

    protected List<TransactionsTagsRecord> transactionsTagsRecords;
    protected List<AccountsRecord> accountsRecords;
    protected List<CurrenciesRecord> currenciesRecords;
    protected Map<String, String> userLang;

    protected Map<Long, TransactionsTagsRecord> transactionsTagsMap;
    protected Map<Long, AccountsRecord> accountsMap;
    protected Map<Long, CurrenciesRecord> currenciesMap;

    public AbstractReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker) {
        this.reportsRecord = reportsRecord;
        int userId = reportsRecord.getUserId();

        this.transactionsTagsRecords = databaseWorker.get(TransactionTagDatabase.class).getTags(userId);
        this.accountsRecords = databaseWorker.get(AccountDatabase.class).getAccounts(userId);
        this.currenciesRecords = databaseWorker.get(CurrencyDatabase.class).getUserCurrenciesWithRoot(userId);

        this.transactionsTagsMap = transactionsTagsRecords.stream()
                .collect(Collectors.toMap(TransactionsTagsRecord::getId, Function.identity()));

        this.accountsMap = accountsRecords.stream()
                .collect(Collectors.toMap(AccountsRecord::getId, Function.identity()));

        this.currenciesMap = currenciesRecords.stream()
                .collect(Collectors.toMap(CurrenciesRecord::getId, Function.identity()));

        JSONB userLangRaw = reportsRecord.getLang();

        if (userLangRaw == null) {
            this.userLang = new HashMap<>();
            return;
        }

        this.userLang = GSON.fromJson(userLangRaw.data(), new TypeToken<Map<String, String>>(){}.getType());

        if (this.userLang == null)
            this.userLang = new HashMap<>();
    }

    protected String getTransactionTagFullPath(long id) {
        TransactionsTagsRecord record = transactionsTagsMap.get(id);

        if (record == null)
            throw new IllegalArgumentException();

        String parentsRaw = record.getParentsTree().data();

        if (parentsRaw.isBlank())
            return record.getName();

        String parents = Arrays
                .stream(parentsRaw.split("\\."))
                .map((entry) -> transactionsTagsMap.get(Long.parseLong(entry)).getName())
                .collect(Collectors.joining(" > "));

        return parents + " > " + record.getName();
    }

    abstract public void consider(List<Record> records);
    abstract public void done();
}

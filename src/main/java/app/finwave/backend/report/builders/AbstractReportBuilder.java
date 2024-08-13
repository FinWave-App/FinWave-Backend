package app.finwave.backend.report.builders;

import app.finwave.backend.api.category.CategoryDatabase;
import app.finwave.backend.api.files.FilesManager;
import app.finwave.backend.jooq.tables.records.CategoriesRecord;
import com.google.gson.reflect.TypeToken;
import io.netty.handler.codec.DateFormatter;
import org.jooq.JSONB;
import org.jooq.Record;
import app.finwave.backend.api.account.AccountDatabase;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.AccountsRecord;
import app.finwave.backend.jooq.tables.records.CurrenciesRecord;
import app.finwave.backend.jooq.tables.records.ReportsRecord;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static app.finwave.backend.api.ApiResponse.GSON;

public abstract class AbstractReportBuilder {
    protected ReportsRecord reportsRecord;

    protected FilesManager filesManager;

    protected List<CategoriesRecord> categoriesRecords;
    protected List<AccountsRecord> accountsRecords;
    protected List<CurrenciesRecord> currenciesRecords;
    protected Map<String, String> userLang;

    protected Map<Long, CategoriesRecord> categoriesMap;
    protected Map<Long, AccountsRecord> accountsMap;
    protected Map<Long, CurrenciesRecord> currenciesMap;

    public AbstractReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker, FilesManager filesManager) {
        this.reportsRecord = reportsRecord;
        this.filesManager = filesManager;

        int userId = reportsRecord.getUserId();

        this.categoriesRecords = databaseWorker.get(CategoryDatabase.class).getCategories(userId);
        this.accountsRecords = databaseWorker.get(AccountDatabase.class).getAccounts(userId);
        this.currenciesRecords = databaseWorker.get(CurrencyDatabase.class).getUserCurrenciesWithRoot(userId);

        this.categoriesMap = categoriesRecords.stream()
                .collect(Collectors.toMap(CategoriesRecord::getId, Function.identity()));

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

    protected String getCategoryFullPath(long id) {
        CategoriesRecord record = categoriesMap.get(id);

        if (record == null)
            throw new IllegalArgumentException();

        String parentsRaw = record.getParentsTree().data();

        if (parentsRaw.isBlank())
            return record.getName();

        String parents = Arrays
                .stream(parentsRaw.split("\\."))
                .map((entry) -> categoriesMap.get(Long.parseLong(entry)).getName())
                .collect(Collectors.joining(" > "));

        return parents + " > " + record.getName();
    }

    protected String getFileName() {
        String filename = reportsRecord.getDescription() == null ? DateFormatter.format(Date.from(Instant.now())) : reportsRecord.getDescription();

        return filename
                .replace(',', '_')
                .replace(' ', '_') + ".csv";
    }

    abstract public void consider(List<Record> records);
    abstract public void done();
}

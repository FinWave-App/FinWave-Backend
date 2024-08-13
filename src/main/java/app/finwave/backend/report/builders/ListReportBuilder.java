package app.finwave.backend.report.builders;

import app.finwave.backend.api.files.FilesManager;
import org.jooq.Record;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.ReportsRecord;

import java.io.*;
import java.math.BigDecimal;
import java.security.DigestOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

public class ListReportBuilder extends AbstractReportBuilder {
    protected OutputStream stream;
    protected BufferedWriter writer;

    protected DateTimeFormatter dateFormatter;

    protected static final String[] head = new String[]{
            "account", "delta", "category", "currency", "created", "description"
    };

    public ListReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker, FilesManager filesManager) throws IOException {
        super(reportsRecord, databaseWorker, filesManager);

        stream = filesManager.getAndOpenStream(reportsRecord.getFileId(), "text/csv", getFileName(), reportsRecord.getDescription()).orElseThrow();
        writer = new BufferedWriter(new OutputStreamWriter(stream));

        this.dateFormatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(
                        Locale.forLanguageTag(userLang.getOrDefault("dateLocale", "en-US"))
                );

        for (String headEntry : head) {
            writer.write(userLang.getOrDefault(headEntry, headEntry));
            writer.write(',');
        }

        writer.newLine();
    }

    @Override
    public void consider(List<Record> records) {
        try {
            for (Record record : records) {
                writer.write(buildLine(record));
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String buildLine(Record record) {
        long accountId = record.get(TRANSACTIONS.ACCOUNT_ID);
        long tagId = record.get(TRANSACTIONS.CATEGORY_ID);
        long currencyId = record.get(TRANSACTIONS.CURRENCY_ID);
        BigDecimal delta = record.get(TRANSACTIONS.DELTA);
        String created = dateFormatter.format(record.get(TRANSACTIONS.CREATED_AT));
        String description = record.get(TRANSACTIONS.DESCRIPTION);

        String account = accountsMap.get(accountId).getName();
        String category = getCategoryFullPath(tagId);
        String currency = currenciesMap.get(currencyId).getCode();

        return account + ',' +
                delta + ',' +
                category + ',' +
                currency + ',' +
                created.replace(',', ' ') + ',' +
                (description == null ? "" : description);
    }

    @Override
    public void done() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package app.finwave.backend.report.builders;

import org.jooq.Record;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.report.ReportFileWorker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

public class ListReportBuilder extends AbstractReportBuilder {
    protected String token;
    protected BufferedWriter writer;
    protected DateTimeFormatter dateFormatter;

    protected static final String[] head = new String[]{
            "account", "delta", "tag", "currency", "created", "description"
    };

    public ListReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker) throws IOException {
        super(reportsRecord, databaseWorker);
        this.token = reportsRecord.getId();
        this.writer = new BufferedWriter(new FileWriter(ReportFileWorker.create(token)));
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
        long tagId = record.get(TRANSACTIONS.TAG_ID);
        long currencyId = record.get(TRANSACTIONS.CURRENCY_ID);
        BigDecimal delta = record.get(TRANSACTIONS.DELTA);
        String created = dateFormatter.format(record.get(TRANSACTIONS.CREATED_AT));
        String description = record.get(TRANSACTIONS.DESCRIPTION);

        String account = accountsMap.get(accountId).getName();
        String tag = getTransactionTagFullPath(tagId);
        String currency = currenciesMap.get(currencyId).getCode();

        return account + ',' +
                delta + ',' +
                tag + ',' +
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

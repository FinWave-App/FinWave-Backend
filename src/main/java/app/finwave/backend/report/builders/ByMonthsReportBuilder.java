package app.finwave.backend.report.builders;

import app.finwave.backend.api.files.FilesManager;
import org.jooq.Record;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.ReportsRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

public class ByMonthsReportBuilder extends ByDaysReportBuilder {
    public ByMonthsReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker, FilesManager filesManager) {
        super(reportsRecord, databaseWorker, filesManager);

        this.formatter = DateTimeFormatter.ofPattern("MM/yyyy",
                Locale.forLanguageTag(userLang.getOrDefault("dateLocale", "en-US"))
        );
    }

    @Override
    public void consider(List<Record> records) {
        for (Record record : records) {
            long categoryId = record.get(TRANSACTIONS.CATEGORY_ID);
            long currencyId = record.get(TRANSACTIONS.CURRENCY_ID);
            BigDecimal delta = record.get(TRANSACTIONS.DELTA);
            LocalDate created = record.get(TRANSACTIONS.CREATED_AT).toLocalDate().withDayOfMonth(1);

            addToCell(currencyId, categoryId, created, delta);
        }
    }
}

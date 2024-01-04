package su.knst.finwave.report.builders;

import org.jooq.Record;
import su.knst.finwave.database.DatabaseWorker;
import su.knst.finwave.jooq.tables.records.ReportsRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS;

public class ByMonthsReportBuilder extends ByDaysReportBuilder {
    public ByMonthsReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker) {
        super(reportsRecord, databaseWorker);

        this.formatter = DateTimeFormatter.ofPattern("MM/yyyy",
                Locale.forLanguageTag(userLang.getOrDefault("dateLocale", "en-US"))
        );
    }

    @Override
    public void consider(List<Record> records) {
        for (Record record : records) {
            long tagId = record.get(TRANSACTIONS.TAG_ID);
            long currencyId = record.get(TRANSACTIONS.CURRENCY_ID);
            BigDecimal delta = record.get(TRANSACTIONS.DELTA);
            LocalDate created = record.get(TRANSACTIONS.CREATED_AT).toLocalDate().withDayOfMonth(1);

            addToCell(currencyId, tagId, created, delta);
        }
    }
}

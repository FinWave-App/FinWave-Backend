package app.finwave.backend.report.builders;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Record;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.CurrenciesRecord;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.report.ReportFileWorker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

public class ByDaysReportBuilder extends AbstractReportBuilder {
    protected String token;
    protected HashMap<Long, TableContent> content = new HashMap<>();
    protected DateTimeFormatter formatter;

    public ByDaysReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker) {
        super(reportsRecord, databaseWorker);
        this.token = reportsRecord.getId();

        this.formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(
                        Locale.forLanguageTag(userLang.getOrDefault("dateLocale", "en-US"))
                );
    }

    @Override
    public void consider(List<Record> records) {
        for (Record record : records) {
            long tagId = record.get(TRANSACTIONS.TAG_ID);
            long currencyId = record.get(TRANSACTIONS.CURRENCY_ID);
            BigDecimal delta = record.get(TRANSACTIONS.DELTA);
            LocalDate created = record.get(TRANSACTIONS.CREATED_AT).toLocalDate();

            addToCell(currencyId, tagId, created, delta);
        }
    }

    @Override
    public void done() {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(ReportFileWorker.create(token)));

            for (Map.Entry<Long, TableContent> entry : content.entrySet()) {
                CurrenciesRecord currenciesRecord = currenciesMap.get(entry.getKey());
                String code = currenciesRecord.getCode();
                int decimals = currenciesRecord.getDecimals();

                TableContent table = entry.getValue();

                String[][] tableContent = buildTable(table, code, decimals);

                for (String[] row : tableContent) {
                    for (String cell : row) {
                        writer.write((cell == null ? "" : cell) + ',');
                    }
                    writer.newLine();
                }

                writer.newLine();
                writer.flush();
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String[][] buildTable(TableContent table, String currencyCode, int decimals) {
        List<Long> tags = table.cells.keySet()
                .stream()
                .map(Pair::getKey)
                .collect(Collectors.toSet())
                .stream().toList();

        List<LocalDate> dates = table.cells.keySet()
                .stream()
                .map(Pair::getValue)
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .toList();

        String[][] tableContent = new String[tags.size() + 2][dates.size() + 2]; // bottom and right total + heads

        tableContent[0][0] = currencyCode;
        putHeadsAndTags(tableContent, tags, dates, formatter);

        HashMap<LocalDate, BigDecimal> sumByDate = new HashMap<>();
        for (int row = 1; row < tableContent.length - 1; row++) {
            long tag = tags.get(row - 1);

            BigDecimal columnTotal = BigDecimal.ZERO;

            for (int column = 1; column < tableContent[row].length - 1; column++) {
                LocalDate date = dates.get(column - 1);
                BigDecimal sum = table.cells.get(Pair.of(tag, date));

                if (sum == null)
                    sum = BigDecimal.ZERO;

                columnTotal = columnTotal.add(sum);

                sumByDate.put(date, sumByDate.getOrDefault(date, BigDecimal.ZERO).add(sum));

                tableContent[row][column] = sum.setScale(decimals, RoundingMode.HALF_DOWN).toString();
            }

            tableContent[row][tableContent[row].length - 1] = columnTotal.setScale(decimals, RoundingMode.HALF_DOWN).toString();
        }

        int dateIndex = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (LocalDate date : dates) {
            BigDecimal decimal = sumByDate.getOrDefault(date, BigDecimal.ZERO);
            tableContent[tags.size() + 1][dateIndex + 1] = decimal.setScale(decimals, RoundingMode.HALF_DOWN).toString();

            total = total.add(decimal);
            dateIndex++;
        }

        tableContent[tags.size() + 1][dates.size() + 1] = total.setScale(decimals, RoundingMode.HALF_DOWN).toString();

        return tableContent;
    }

    protected void putHeadsAndTags(String[][] table, List<Long> tags, List<LocalDate> dates, DateTimeFormatter formatter) {
        int i = 1;
        String total = userLang.getOrDefault("total", "Total");

        for (LocalDate date : dates) {
            table[0][i] = date.format(formatter);

            i++;
        }
        table[0][i] = total;

        i = 1;

        for (Long tag : tags) {
            table[i][0] = getTransactionTagFullPath(tag);

            i++;
        }
        table[i][0] = total;
    }

    protected void addToCell(long currency, long tag, LocalDate date, BigDecimal delta) {
        TableContent tableContent = content.get(currency);

        if (tableContent == null) {
            tableContent = new TableContent();

            content.put(currency, tableContent);
        }

        var pair = Pair.of(tag, date);
        BigDecimal oldValue = tableContent.cells.getOrDefault(pair, BigDecimal.ZERO);

        tableContent.cells.put(pair, oldValue.add(delta));
    }

    static class TableContent {
        public final HashMap<Pair<Long, LocalDate>, BigDecimal> cells = new HashMap<>();
    }
}

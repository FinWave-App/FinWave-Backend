package app.finwave.backend.report.builders;

import app.finwave.backend.api.files.FilesManager;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import io.netty.handler.codec.DateFormatter;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Record;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.CurrenciesRecord;
import app.finwave.backend.jooq.tables.records.ReportsRecord;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;

import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

public class ByDaysReportBuilder extends AbstractReportBuilder {
    protected HashMap<Long, TableContent> content = new HashMap<>();
    protected DateTimeFormatter formatter;

    public ByDaysReportBuilder(ReportsRecord reportsRecord, DatabaseWorker databaseWorker, FilesManager filesManager) {
        super(reportsRecord, databaseWorker, filesManager);

        this.formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(
                        Locale.forLanguageTag(userLang.getOrDefault("dateLocale", "en-US"))
                );
    }

    @Override
    public void consider(List<Record> records) {
        for (Record record : records) {
            long categoryId = record.get(TRANSACTIONS.CATEGORY_ID);
            long currencyId = record.get(TRANSACTIONS.CURRENCY_ID);
            BigDecimal delta = record.get(TRANSACTIONS.DELTA);
            LocalDate created = record.get(TRANSACTIONS.CREATED_AT).toLocalDate();

            addToCell(currencyId, categoryId, created, delta);
        }
    }

    @Override
    public void done() {
        BufferedWriter writer;
        try {
            OutputStream stream = filesManager.getAndOpenStream(reportsRecord.getFileId(), "text/csv", getFileName(), reportsRecord.getDescription()).orElseThrow();

            writer = new BufferedWriter(new OutputStreamWriter(stream));

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
        List<Long> categories = table.cells.keySet()
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

        String[][] tableContent = new String[categories.size() + 2][dates.size() + 2]; // bottom and right total + heads

        tableContent[0][0] = currencyCode;
        putHeadsAndCategories(tableContent, categories, dates, formatter);

        HashMap<LocalDate, BigDecimal> sumByDate = new HashMap<>();
        for (int row = 1; row < tableContent.length - 1; row++) {
            long category = categories.get(row - 1);

            BigDecimal columnTotal = BigDecimal.ZERO;

            for (int column = 1; column < tableContent[row].length - 1; column++) {
                LocalDate date = dates.get(column - 1);
                BigDecimal sum = table.cells.get(Pair.of(category, date));

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
            tableContent[categories.size() + 1][dateIndex + 1] = decimal.setScale(decimals, RoundingMode.HALF_DOWN).toString();

            total = total.add(decimal);
            dateIndex++;
        }

        tableContent[categories.size() + 1][dates.size() + 1] = total.setScale(decimals, RoundingMode.HALF_DOWN).toString();

        return tableContent;
    }

    protected void putHeadsAndCategories(String[][] table, List<Long> categories, List<LocalDate> dates, DateTimeFormatter formatter) {
        int i = 1;
        String total = userLang.getOrDefault("total", "Total");

        for (LocalDate date : dates) {
            table[0][i] = date.format(formatter);

            i++;
        }
        table[0][i] = total;

        i = 1;

        for (Long category : categories) {
            table[i][0] = getCategoryFullPath(category);

            i++;
        }
        table[i][0] = total;
    }

    protected void addToCell(long currency, long category, LocalDate date, BigDecimal delta) {
        TableContent tableContent = content.get(currency);

        if (tableContent == null) {
            tableContent = new TableContent();

            content.put(currency, tableContent);
        }

        var pair = Pair.of(category, date);
        BigDecimal oldValue = tableContent.cells.getOrDefault(pair, BigDecimal.ZERO);

        tableContent.cells.put(pair, oldValue.add(delta));
    }

    static class TableContent {
        public final HashMap<Pair<Long, LocalDate>, BigDecimal> cells = new HashMap<>();
    }
}

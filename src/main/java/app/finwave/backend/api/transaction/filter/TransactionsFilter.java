package app.finwave.backend.api.transaction.filter;

import spark.Request;
import app.finwave.backend.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TransactionsFilter {
    public static final TransactionsFilter EMPTY = new TransactionsFilter();

    protected final List<Long> categoriesIds;
    protected final List<Long> accountIds;
    protected final List<Long> currenciesIds;
    protected final OffsetDateTime fromTime;
    protected final OffsetDateTime toTime;
    protected final String description;

    protected TransactionsFilter() {
        this((List<Long>) null, null, null, null, null, null);
    }

    public TransactionsFilter(List<Long> categoriesIds, List<Long> accountIds, List<Long> currenciesIds, OffsetDateTime fromTime, OffsetDateTime toTime, String description) {
        this.categoriesIds = categoriesIds;
        this.accountIds = accountIds;
        this.currenciesIds = currenciesIds;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.description = description;
    }

    public TransactionsFilter(String categoriesIdsRaw, String accountIdsRaw, String currenciesIdsRaw, String fromTimeRaw, String toTimeRaw, String description) {
        this(parseIds(categoriesIdsRaw),
                parseIds(accountIdsRaw),
                parseIds(currenciesIdsRaw),
                fromTimeRaw != null ? OffsetDateTime.parse(fromTimeRaw) : null,
                toTimeRaw != null ? OffsetDateTime.parse(toTimeRaw) : null,
                description
        );
    }

    public TransactionsFilter(Request request) {
        this(request.queryParams("categoriesIds"),
                request.queryParams("accountsIds"),
                request.queryParams("currenciesIds"),
                request.queryParams("fromTime"),
                request.queryParams("toTime"),
                request.queryParams("description"));
    }

    public boolean validateTime(double maxDaysRange) {
        if (fromTime == null || toTime == null)
            return false;

        return fromTime.isBefore(toTime) && (Math.floor((toTime.toEpochSecond() - fromTime.toEpochSecond()) / 86400d) <= maxDaysRange);
    }

    protected static List<Long> parseIds(String raw) {
        if (raw == null)
            return null;

        String[] array = raw.split(",");

        return Arrays.stream(array)
                .map(ParamsValidator::longV)
                .map((v) -> v.range(1, Long.MAX_VALUE).require())
                .toList();
    }

    public List<Long> getCategoriesIds() {
        return categoriesIds;
    }

    public List<Long> getAccountIds() {
        return accountIds;
    }

    public List<Long> getCurrenciesIds() {
        return currenciesIds;
    }

    public OffsetDateTime getFromTime() {
        return fromTime;
    }

    public OffsetDateTime getToTime() {
        return toTime;
    }

    public String getDescription() {
        return description;
    }

    public TransactionsFilter setCategoriesIds(List<Long> categoriesIds) {
        return new TransactionsFilter(categoriesIds, accountIds, currenciesIds, fromTime, toTime, description);
    }

    public TransactionsFilter setAccountIds(List<Long> accountIds) {
        return new TransactionsFilter(categoriesIds, accountIds, currenciesIds, fromTime, toTime, description);
    }

    public TransactionsFilter setCurrenciesIds(List<Long> currenciesIds) {
        return new TransactionsFilter(categoriesIds, accountIds, currenciesIds, fromTime, toTime, description);
    }

    public TransactionsFilter setFromTime(OffsetDateTime fromTime) {
        return new TransactionsFilter(categoriesIds, accountIds, currenciesIds, fromTime, toTime, description);
    }

    public TransactionsFilter setToTime(OffsetDateTime toTime) {
        return new TransactionsFilter(categoriesIds, accountIds, currenciesIds, fromTime, toTime, description);
    }

    public TransactionsFilter setDescription(String description) {
        return new TransactionsFilter(categoriesIds, accountIds, currenciesIds, fromTime, toTime, description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionsFilter that = (TransactionsFilter) o;
        return Objects.equals(categoriesIds, that.categoriesIds) && Objects.equals(accountIds, that.accountIds) && Objects.equals(currenciesIds, that.currenciesIds) && Objects.equals(fromTime, that.fromTime) && Objects.equals(toTime, that.toTime) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoriesIds, accountIds, currenciesIds, fromTime, toTime, description);
    }
}

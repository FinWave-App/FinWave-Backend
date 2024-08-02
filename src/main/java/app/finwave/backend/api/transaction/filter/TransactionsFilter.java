package app.finwave.backend.api.transaction.filter;

import spark.Request;
import app.finwave.backend.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TransactionsFilter {
    public static final TransactionsFilter EMPTY = new TransactionsFilter();

    protected List<Long> tagsIds;
    protected List<Long> accountIds;
    protected List<Long> currenciesIds;
    protected OffsetDateTime fromTime;
    protected OffsetDateTime toTime;
    protected String description;

    protected TransactionsFilter() {
    }

    public TransactionsFilter(List<Long> tagsIds, List<Long> accountIds, List<Long> currenciesIds, OffsetDateTime fromTime, OffsetDateTime toTime, String description) {
        this.tagsIds = tagsIds;
        this.accountIds = accountIds;
        this.currenciesIds = currenciesIds;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.description = description;
    }

    public TransactionsFilter(String tagsIdRaw, String accountIdsRaw, String currenciesIdsRaw, String fromTimeRaw, String toTimeRaw, String description) {
        this(parseIds(tagsIdRaw),
                parseIds(accountIdsRaw),
                parseIds(currenciesIdsRaw),
                fromTimeRaw != null ? OffsetDateTime.parse(fromTimeRaw) : null,
                toTimeRaw != null ? OffsetDateTime.parse(toTimeRaw) : null,
                description
        );
    }

    public TransactionsFilter(Request request) {
        this(request.queryParams("tagsIds"),
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

    public List<Long> getTagsIds() {
        return tagsIds;
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

    public void setTagsIds(List<Long> tagsIds) {
        this.tagsIds = tagsIds;
    }

    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }

    public void setCurrenciesIds(List<Long> currenciesIds) {
        this.currenciesIds = currenciesIds;
    }

    public void setFromTime(OffsetDateTime fromTime) {
        this.fromTime = fromTime;
    }

    public void setToTime(OffsetDateTime toTime) {
        this.toTime = toTime;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionsFilter that = (TransactionsFilter) o;
        return Objects.equals(tagsIds, that.tagsIds) && Objects.equals(accountIds, that.accountIds) && Objects.equals(currenciesIds, that.currenciesIds) && Objects.equals(fromTime, that.fromTime) && Objects.equals(toTime, that.toTime) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagsIds, accountIds, currenciesIds, fromTime, toTime, description);
    }
}

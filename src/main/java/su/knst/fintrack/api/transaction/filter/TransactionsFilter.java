package su.knst.fintrack.api.transaction.filter;

import spark.Request;
import su.knst.fintrack.utils.params.ParamsValidator;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public class TransactionsFilter {
    public static final TransactionsFilter EMPTY = new TransactionsFilter();

    protected List<Long> tagsIds;
    protected List<Long> accountIds;
    protected List<Long> currenciesIds;
    protected OffsetDateTime fromTime;
    protected OffsetDateTime toTime;
    protected String descriptionContains;

    protected TransactionsFilter() {
    }

    public TransactionsFilter(List<Long> tagsIds, List<Long> accountIds, List<Long> currenciesIds, OffsetDateTime fromTime, OffsetDateTime toTime, String descriptionContains) {
        this.tagsIds = tagsIds;
        this.accountIds = accountIds;
        this.currenciesIds = currenciesIds;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.descriptionContains = descriptionContains;
    }

    public TransactionsFilter(String tagsIdRaw, String accountIdsRaw, String currenciesIdsRaw, String fromTimeRaw, String toTimeRaw, String descriptionContains) {
        this.tagsIds = parseIds(tagsIdRaw);
        this.accountIds = parseIds(accountIdsRaw);
        this.currenciesIds = parseIds(currenciesIdsRaw);

        if (fromTimeRaw != null)
            fromTime = OffsetDateTime.parse(fromTimeRaw);

        if (toTimeRaw != null)
            toTime = OffsetDateTime.parse(toTimeRaw);

        this.descriptionContains = descriptionContains;
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

        return fromTime.isBefore(toTime) && ((toTime.toEpochSecond() - fromTime.toEpochSecond()) / 86400d <= maxDaysRange);
    }

    protected List<Long> parseIds(String raw) {
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

    public String getDescriptionContains() {
        return descriptionContains;
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

    public void setDescriptionContains(String descriptionContains) {
        this.descriptionContains = descriptionContains;
    }
}

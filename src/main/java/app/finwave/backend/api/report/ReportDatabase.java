package app.finwave.backend.api.report;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import app.finwave.backend.api.report.data.ReportStatus;
import app.finwave.backend.api.report.data.ReportType;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.utils.TokenGenerator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static app.finwave.backend.api.ApiResponse.GSON;
import static app.finwave.backend.jooq.Tables.REPORTS;

public class ReportDatabase extends AbstractDatabase {
    public ReportDatabase(DSLContext context) {
        super(context);
    }

    public String newReport(String description, TransactionsFilter filter, Map<String, String> langMap, ReportType type, int userId, int expiresDays) {
        String token = TokenGenerator.generateReportToken();
        OffsetDateTime now = OffsetDateTime.now();

        context.insertInto(REPORTS)
                .set(REPORTS.ID, token)
                .set(REPORTS.DESCRIPTION, description)
                .set(REPORTS.STATUS, ReportStatus.IN_PROGRESS.getShort())
                .set(REPORTS.TYPE, (short) type.ordinal())
                .set(REPORTS.FILTER, JSONB.valueOf(GSON.toJson(filter)))
                .set(REPORTS.LANG, JSONB.valueOf(GSON.toJson(langMap)))
                .set(REPORTS.USER_ID, userId)
                .set(REPORTS.CREATED_AT, now)
                .set(REPORTS.EXPIRES_AT, now.plusDays(expiresDays))
                .execute();

        return token;
    }

    public void updateReport(String token, ReportStatus status) {
        context.update(REPORTS)
                .set(REPORTS.STATUS, status.getShort())
                .where(REPORTS.ID.eq(token))
                .execute();
    }

    public Optional<ReportsRecord> getReport(String token) {
        return context.selectFrom(REPORTS)
                .where(REPORTS.ID.eq(token))
                .fetchOptional();
    }

    public List<ReportsRecord> getReports(int userId) {
        return context.selectFrom(REPORTS)
                .where(REPORTS.USER_ID.eq(userId))
                .orderBy(REPORTS.CREATED_AT.desc())
                .fetch();
    }

    public List<ReportsRecord> getToRemove(int count) {
        return context.selectFrom(REPORTS)
                .where(REPORTS.EXPIRES_AT.lessOrEqual(OffsetDateTime.now()))
                .limit(count)
                .fetch();
    }

    public void removeReport(String token) {
        context.deleteFrom(REPORTS)
                .where(REPORTS.ID.eq(token))
                .execute();
    }
}

package app.finwave.backend.api.report;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import app.finwave.backend.api.report.data.ReportStatus;
import app.finwave.backend.api.report.data.ReportType;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.utils.TokenGenerator;
import org.jooq.Record1;

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

    public long newReport(String description, TransactionsFilter filter, Map<String, String> langMap, ReportType type, int userId, String fileId) {
        return context.insertInto(REPORTS)
                .set(REPORTS.DESCRIPTION, description)
                .set(REPORTS.STATUS, ReportStatus.IN_PROGRESS.getShort())
                .set(REPORTS.TYPE, (short) type.ordinal())
                .set(REPORTS.FILTER, JSONB.valueOf(GSON.toJson(filter)))
                .set(REPORTS.LANG, JSONB.valueOf(GSON.toJson(langMap)))
                .set(REPORTS.USER_ID, userId)
                .set(REPORTS.FILE_ID, fileId)
                .returningResult(REPORTS.ID)
                .fetchOptional()
                .map(Record1::component1)
                .orElse(-1L);
    }

    public Optional<ReportsRecord> getReportByFile(String fileId) {
        return context.selectFrom(REPORTS)
                .where(REPORTS.FILE_ID.eq(fileId))
                .fetchOptional();
    }

    public void updateReport(long reportId, ReportStatus status) {
        context.update(REPORTS)
                .set(REPORTS.STATUS, status.getShort())
                .where(REPORTS.ID.eq(reportId))
                .execute();
    }

    public Optional<ReportsRecord> getReport(long reportId) {
        return context.selectFrom(REPORTS)
                .where(REPORTS.ID.eq(reportId))
                .fetchOptional();
    }

    public List<ReportsRecord> getReports(int userId) {
        return context.selectFrom(REPORTS)
                .where(REPORTS.USER_ID.eq(userId))
                .orderBy(REPORTS.ID.desc())
                .fetch();
    }

    public void removeReport(long reportId) {
        context.deleteFrom(REPORTS)
                .where(REPORTS.ID.eq(reportId))
                .execute();
    }
}

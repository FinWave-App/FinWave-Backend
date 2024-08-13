package app.finwave.backend.api.report;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import app.finwave.backend.api.files.FilesManager;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.DateFormatter;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.report.data.ReportType;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.ReportConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.report.ReportBuilder;
import app.finwave.backend.utils.params.ParamsValidator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class ReportApi {
    protected ReportConfig config;

    protected ReportDatabase database;
    protected ReportBuilder builder;

    protected FilesManager filesManager;

    protected WebSocketWorker socketWorker;

    @Inject
    public ReportApi(Configs configs, DatabaseWorker databaseWorker, ReportBuilder builder, WebSocketWorker socketWorker, FilesManager filesManager) {
        this.config = configs.getState(new ReportConfig());
        this.database = databaseWorker.get(ReportDatabase.class);
        this.builder = builder;

        this.socketWorker = socketWorker;
        this.filesManager = filesManager;

        filesManager.addFileDeletionListener((r) -> {
            Optional<ReportsRecord> optionalReport = database.getReportByFile(r.getId());

            if (optionalReport.isEmpty())
                return;

            int userId = optionalReport.get().getUserId();

            database.removeReport(optionalReport.get().getId());
            socketWorker.sendToUser(userId, new NotifyUpdate("reports"));
        });
    }

    public Object newReport(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        ReportRequest reportRequest = ParamsValidator.bodyObject(request, ReportRequest.class)
                .matches((r) -> r.description == null || !r.description.isBlank() && r.description.length() <= config.maxDescriptionLength)
                .matches((r) -> r.type >= 0 && r.type < ReportType.values().length)
                .matches(ReportRequest::validateLang)
                .require();

        Optional<FilesRecord> filesRecord = filesManager.registerNewEmptyFile(sessionRecord.getUserId(), config.expiresDays, true, "reports");

        if (filesRecord.isEmpty())
            halt(500);

        long reportId = database.newReport(
                reportRequest.description,
                reportRequest.filter,
                reportRequest.lang,
                ReportType.values()[reportRequest.type],
                sessionRecord.getUserId(),
                filesRecord.get().getId()
        );

        builder.buildAsync(reportId).whenComplete((r, t) -> {
            socketWorker.sendToUser(sessionRecord.getUserId(), new NotifyUpdate("reports"));
        });

        response.status(202);

        return new NewReportResponse(reportId, filesRecord.get().getId());
    }

    public Object getList(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        List<ReportsRecord> records = database.getReports(sessionRecord.getUserId());

        response.status(200);

        return new GetListResponse(records);
    }

    record ReportRequest(String description, TransactionsFilter filter, int type, Map<String, String> lang) {
        public boolean validateLang() {
            if (lang == null)
                return true;

            for (Map.Entry<String, String> e : lang.entrySet()) {
                if (e.getKey().length() > 256 || e.getValue().length() > 256)
                    return false;
            }

            return true;
        }
    }

    static class GetListResponse extends ApiResponse {
        public final List<Entry> reports;

        public GetListResponse(List<ReportsRecord> records) {
            this.reports = records.stream()
                    .map((r) -> new Entry(
                            r.getId(),
                            r.getDescription(),
                            r.getStatus(),
                            r.getType(),
                            r.getFileId()
                    )).toList();
        }

        record Entry(long reportId, String description, short status, short type, String fileId) {}
    }

    static class NewReportResponse extends ApiResponse {
        public final long reportId;

        public NewReportResponse(long reportId, String fileId) {
            this.reportId = reportId;
        }
    }
}
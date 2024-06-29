package app.finwave.backend.api.report;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.DateFormatter;
import spark.Request;
import spark.Response;
import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.accumulation.AccumulationApi;
import app.finwave.backend.api.report.data.ReportType;
import app.finwave.backend.api.session.SessionApi;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.app.ReportConfig;
import app.finwave.backend.config.general.ReportBuilderConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.report.ReportBuilder;
import app.finwave.backend.report.ReportFileWorker;
import app.finwave.backend.utils.params.ParamsValidator;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;

import static spark.Spark.halt;
import static app.finwave.backend.utils.TokenGenerator.generateSessionToken;

@Singleton
public class ReportApi {
    protected ReportConfig config;

    protected ReportDatabase database;
    protected ReportBuilder builder;

    @Inject
    public ReportApi(Configs configs, DatabaseWorker databaseWorker, ReportBuilder builder) {
        this.config = configs.getState(new ReportConfig());
        this.database = databaseWorker.get(ReportDatabase.class);
        this.builder = builder;
    }

    public Object newReport(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        ReportRequest reportRequest = ParamsValidator.bodyObject(request, ReportRequest.class)
                .matches((r) -> r.description == null || !r.description.isBlank() && r.description.length() <= config.maxDescriptionLength)
                .matches((r) -> r.type >= 0 && r.type < ReportType.values().length)
                .matches(ReportRequest::validateLang)
                .require();

        String token = database.newReport(
                reportRequest.description,
                reportRequest.filter,
                reportRequest.lang,
                ReportType.values()[reportRequest.type],
                sessionRecord.getUserId(),
                config.expiresDays
        );

        builder.buildAsync(token);

        response.status(202);

        return new NewReportResponse(token);
    }

    public Object getList(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        List<ReportsRecord> records = database.getReports(sessionRecord.getUserId());

        response.status(200);

        return new GetListResponse(records);
    }

    public Object downloadReport(Request request, Response response) {
        String token = ParamsValidator
                .string(request, "token")
                .require();

        Optional<ReportsRecord> record = database.getReport(token);
        if (record.isEmpty() || record.get().getStatus() != 1)
            halt(400);

        Optional<File> optionalFile = ReportFileWorker.get(token);
        if (optionalFile.isEmpty())
            halt(500);

        File reportFile = optionalFile.get();

        String filename = record.get().getDescription() == null ? DateFormatter.format(Date.from(record.get().getCreatedAt().toInstant())) : record.get().getDescription();
        filename = filename
                .replace(',', '_')
                .replace(' ', '_');

        response.header("Content-Type", "text/csv");
        response.header("Content-Disposition", "attachment;filename=" + filename + ".csv");

        try {
            try(BufferedOutputStream outputStream = new BufferedOutputStream(response.raw().getOutputStream());
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(reportFile)))
            {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = bufferedInputStream.read(buffer)) > 0)
                    outputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {
            halt(405);
        }

        return response.raw();
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
                            r.getCreatedAt(),
                            r.getExpiresAt()
                    )).toList();
        }

        record Entry(String token, String description, short status, short type, OffsetDateTime created, OffsetDateTime expires) {}
    }

    static class NewReportResponse extends ApiResponse {
        public final String token;

        public NewReportResponse(String token) {
            this.token = token;
        }
    }
}
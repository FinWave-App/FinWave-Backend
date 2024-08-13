package app.finwave.backend.report;

import app.finwave.backend.api.files.FilesManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Record;
import app.finwave.backend.api.report.ReportDatabase;
import app.finwave.backend.api.report.data.ReportStatus;
import app.finwave.backend.api.report.data.ReportType;
import app.finwave.backend.api.transaction.TransactionDatabase;
import app.finwave.backend.api.transaction.filter.TransactionsFilter;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.ReportBuilderConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.report.builders.AbstractReportBuilder;
import app.finwave.backend.report.builders.ByDaysReportBuilder;
import app.finwave.backend.report.builders.ByMonthsReportBuilder;
import app.finwave.backend.report.builders.ListReportBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static app.finwave.backend.api.ApiResponse.GSON;

@Singleton
public class ReportBuilder {
    protected TransactionDatabase transactionDatabase;
    protected ReportDatabase reportDatabase;
    protected ReportBuilderConfig config;
    protected DatabaseWorker worker;
    protected ExecutorService executor;

    protected FilesManager filesManager;

    @Inject
    public ReportBuilder(DatabaseWorker worker, Configs configs, FilesManager filesManager) {
        this.worker = worker;
        this.filesManager = filesManager;

        this.transactionDatabase = worker.get(TransactionDatabase.class);
        this.reportDatabase = worker.get(ReportDatabase.class);

        this.config = configs.getState(new ReportBuilderConfig());
        this.executor = Executors.newFixedThreadPool(config.threads);
    }

    public CompletableFuture<ReportStatus> buildAsync(long reportId) {
        CompletableFuture<ReportStatus> result = new CompletableFuture<>();

        executor.submit(() -> {
            ReportStatus status = ReportStatus.FAILED;

            try{
                Optional<ReportsRecord> recordOptional = reportDatabase.getReport(reportId);

                if (recordOptional.isEmpty())
                    return;

                ReportsRecord record = recordOptional.get();

                try {
                    status = buildAndSave(record);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                reportDatabase.updateReport(reportId, status);
            }finally {
                result.complete(status);
            }
        });

        return result;
    }

    protected ReportStatus buildAndSave(ReportsRecord record) throws IOException {
        int userId = record.getUserId();
        TransactionsFilter filter = GSON.fromJson(record.getFilter().data(), TransactionsFilter.class);

        if (filter == null)
            filter = TransactionsFilter.EMPTY;

        int offset = 0;
        int count = config.maxTransactionsPerCycle;

        AbstractReportBuilder builder = get(record);

        while (true) {
            List<Record> transactions = transactionDatabase.getTransactions(userId, offset, count, filter);

            if (transactions.isEmpty())
                break;

            offset += transactions.size();

            builder.consider(transactions);
        }

        builder.done();

        return ReportStatus.AVAILABLE;
    }

    protected AbstractReportBuilder get(ReportsRecord reportsRecord) throws IOException {
        ReportType type = ReportType.values()[reportsRecord.getType()];

        switch (type) {
            case BY_DAYS -> {
                return new ByDaysReportBuilder(reportsRecord, worker, filesManager);
            }
            case BY_MONTHS -> {
                return new ByMonthsReportBuilder(reportsRecord, worker, filesManager);
            }
            default -> {
                return new ListReportBuilder(reportsRecord, worker, filesManager);
            }
        }
    }
}

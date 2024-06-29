package app.finwave.backend.api.server;

import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.api.report.ReportApi;
import app.finwave.backend.jooq.tables.records.ReportsRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.VersionCatcher;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;

import java.util.List;

@Singleton
public class ServerApi {
    public Object getVersion(Request request, Response response) {
        return new GetVersionResponse(VersionCatcher.VERSION);
    }

    static class GetVersionResponse extends ApiResponse {
        public final String version;

        public GetVersionResponse(String version) {
            this.version = version;
        }
    }
}

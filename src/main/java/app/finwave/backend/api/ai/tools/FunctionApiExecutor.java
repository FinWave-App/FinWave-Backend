package app.finwave.backend.api.ai.tools;

import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import spark.Request;
import spark.Response;

import java.util.Map;

public interface FunctionApiExecutor extends FunctionExecutor {
    @Override
    default Object run(UsersSessionsRecord session, Map<String, String> args) throws InvalidParameterException {
        FakeRequest request = new FakeRequest(args);
        request.attribute("session", session);

        return api(request, new FakeResponse());
    }

    Object api(Request request, Response response);
}

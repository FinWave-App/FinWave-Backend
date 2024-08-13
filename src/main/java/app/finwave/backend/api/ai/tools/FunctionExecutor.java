package app.finwave.backend.api.ai.tools;

import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;

import java.util.Map;

public interface FunctionExecutor {
    Object run(UsersSessionsRecord session, Map<String, String> args) throws InvalidParameterException;
}

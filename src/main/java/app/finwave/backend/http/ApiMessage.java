package app.finwave.backend.http;

import app.finwave.backend.api.ApiResponse;

public class ApiMessage extends ApiResponse {
    public final String message;

    public static ApiMessage of(String message) {
        return new ApiMessage(message);
    }

    protected ApiMessage(String message) {
        this.message = message;
    }
}

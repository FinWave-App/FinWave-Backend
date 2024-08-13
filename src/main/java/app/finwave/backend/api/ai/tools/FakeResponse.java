package app.finwave.backend.api.ai.tools;

import spark.Response;

public class FakeResponse extends Response {
    protected int fakeStatus;

    @Override
    public void status(int statusCode) {
        this.fakeStatus = statusCode;
    }

    @Override
    public int status() {
        return fakeStatus;
    }
}

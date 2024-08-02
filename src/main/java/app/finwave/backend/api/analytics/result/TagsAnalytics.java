package app.finwave.backend.api.analytics.result;

import app.finwave.backend.api.ApiResponse;

import java.util.List;

public class TagsAnalytics extends ApiResponse {
    public final List<TagSummaryWithManagement> result;

    public TagsAnalytics(List<TagSummaryWithManagement> result) {
        this.result = result;
    }
}

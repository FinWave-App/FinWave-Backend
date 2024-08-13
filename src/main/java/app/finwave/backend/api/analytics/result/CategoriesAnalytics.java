package app.finwave.backend.api.analytics.result;

import app.finwave.backend.api.ApiResponse;

import java.util.List;

public class CategoriesAnalytics extends ApiResponse {
    public final List<CategorySummaryWithBudget> result;

    public CategoriesAnalytics(List<CategorySummaryWithBudget> result) {
        this.result = result;
    }
}

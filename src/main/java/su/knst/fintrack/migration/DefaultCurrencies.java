package su.knst.fintrack.migration;

import java.util.List;

public class DefaultCurrencies {
    public static final List<DefaultCurrency> LIST = List.of(
            new DefaultCurrency("USD", "$", "US Dollar"),
            new DefaultCurrency("EUR", "€", "Euro"),
            new DefaultCurrency("GBP", "£", "Sterling"),
            new DefaultCurrency("JPY", "¥", "Japanese yen"),
            new DefaultCurrency("RUB", "₽", "Russian ruble")
    );

    public record DefaultCurrency(String code, String symbol, String description) {
    }
}

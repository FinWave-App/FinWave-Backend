package app.finwave.backend.migration;

import java.util.List;

public class DefaultCurrencies {
    public static final List<DefaultCurrency> LIST = List.of(
            new DefaultCurrency("USD", "$", "US dollar", 2),
            new DefaultCurrency("EUR", "€", "Euro", 2),
            new DefaultCurrency("GBP", "£", "Sterling", 2),
            new DefaultCurrency("JPY", "¥", "Japanese yen", 2),
            new DefaultCurrency("RUB", "₽", "Russian ruble", 2)
    );

    public record DefaultCurrency(String code, String symbol, String description, int decimals) {
    }
}

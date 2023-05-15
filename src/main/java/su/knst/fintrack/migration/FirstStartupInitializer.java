package su.knst.fintrack.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.knst.fintrack.api.currency.CurrencyDatabase;
import su.knst.fintrack.api.user.UserDatabase;

import java.util.Optional;

@Singleton
public class FirstStartupInitializer {
    protected static final Logger log = LoggerFactory.getLogger(FirstStartupInitializer.class);

    protected UserDatabase userDatabase;
    protected CurrencyDatabase currencyDatabase;

    @Inject
    public FirstStartupInitializer(UserDatabase userDatabase, CurrencyDatabase currencyDatabase) {
        this.userDatabase = userDatabase;
        this.currencyDatabase = currencyDatabase;

        initRoot();
        initCurrencies();
    }

    public void initRoot() {
        if (userDatabase.userExists("root"))
            return;

        Optional<Integer> rootId = userDatabase.registerUser("root", "change_me");

        if (rootId.isEmpty()) {
            log.error("Failed to register root user!");
            return;
        }

        if (rootId.get() != 1) {
            log.error("Root's id not 1: {}", rootId.get());
        }
    }

    public void initCurrencies() {
        if (currencyDatabase.getCurrenciesCount(1) != 0)
            return;

        for (DefaultCurrencies.DefaultCurrency currency : DefaultCurrencies.LIST)
            currencyDatabase.newCurrency(1, currency.code(), currency.symbol(), currency.description());
    }
}

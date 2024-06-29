package app.finwave.backend.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.finwave.backend.api.currency.CurrencyDatabase;
import app.finwave.backend.api.user.UserDatabase;
import app.finwave.backend.config.Configs;
import app.finwave.backend.database.DatabaseWorker;

import java.util.Optional;

@Singleton
public class FirstStartupInitializer {
    protected static final Logger log = LoggerFactory.getLogger(FirstStartupInitializer.class);

    protected UserDatabase userDatabase;
    protected CurrencyDatabase currencyDatabase;

    @Inject
    public FirstStartupInitializer(DatabaseWorker databaseWorker) {
        this.userDatabase = databaseWorker.get(UserDatabase.class);
        this.currencyDatabase = databaseWorker.get(CurrencyDatabase.class);

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
            currencyDatabase.newCurrency(1, currency.code(), currency.symbol(), (short) currency.decimals(), currency.description());
    }
}

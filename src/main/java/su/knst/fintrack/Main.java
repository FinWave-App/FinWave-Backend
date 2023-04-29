package su.knst.fintrack;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.jooq.Constants;
import org.jooq.tools.JooqLogger;
import su.knst.fintrack.api.transaction.tag.TransactionTagDatabase;
import su.knst.fintrack.api.user.UserDatabase;
import su.knst.fintrack.http.HttpWorker;
import su.knst.fintrack.logging.LogsInitializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;

public class Main {
    public static final Injector INJ;

    static {
        INJ = Guice.createInjector(binder -> {

        });
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        LogsInitializer.init();

        INJ.getInstance(HttpWorker.class);
    }
}
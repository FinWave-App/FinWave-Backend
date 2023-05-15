package su.knst.fintrack;

import com.google.inject.Guice;
import com.google.inject.Injector;
import su.knst.fintrack.http.HttpWorker;
import su.knst.fintrack.logging.LogsInitializer;
import su.knst.fintrack.migration.FirstStartupInitializer;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Main {
    public static final Injector INJ;

    static {
        INJ = Guice.createInjector(binder -> {

        });
    }

    public static void main(String[] args) throws IOException {
        LogsInitializer.init();

        INJ.getInstance(FirstStartupInitializer.class);

        INJ.getInstance(HttpWorker.class);
    }
}
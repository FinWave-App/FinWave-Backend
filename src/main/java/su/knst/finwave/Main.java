package su.knst.finwave;

import com.google.inject.Guice;
import com.google.inject.Injector;
import su.knst.finwave.http.HttpWorker;
import su.knst.finwave.logging.LogsInitializer;
import su.knst.finwave.migration.FirstStartupInitializer;

import java.io.IOException;

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
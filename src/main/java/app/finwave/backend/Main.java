package app.finwave.backend;

import com.google.inject.Guice;
import com.google.inject.Injector;
import app.finwave.backend.http.HttpWorker;
import app.finwave.backend.logging.LogsInitializer;
import app.finwave.backend.migration.FirstStartupInitializer;
import app.finwave.backend.service.ServicesManager;

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
        INJ.getInstance(ServicesManager.class);
    }
}
package app.finwave.backend.service.demo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.finwave.backend.api.report.ReportDatabase;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.migration.FirstStartupInitializer;
import app.finwave.backend.migration.Migrator;
import app.finwave.backend.service.AbstractService;

import java.util.concurrent.TimeUnit;

@Singleton
public class DemoService extends AbstractService {
    protected static final Logger log = LoggerFactory.getLogger(DemoService.class);

    protected DSLContext context;
    protected FirstStartupInitializer firstStartupInitializer;
    protected Migrator migrator;

    @Inject
    public DemoService(DatabaseWorker databaseWorker, FirstStartupInitializer firstStartupInitializer, Migrator migrator) {
        this.context = databaseWorker.getDefaultContext();
        this.firstStartupInitializer = firstStartupInitializer;
        this.migrator = migrator;
    }

    @Override
    public void run() {
        context.meta()
                .getSchemas()
                .stream()
                .filter(schema -> schema.getName().equals("public"))
                .flatMap(schema -> schema.getTables().stream())
                .map(Table::getName)
                .filter(tableName -> !tableName.contains("flyway"))
                .forEach(tableName -> {
                            try {
                                context.truncateTable(DSL.table(DSL.name(tableName))).restartIdentity().cascade().execute();
                            }catch (Exception e) {
                                log.error("Fail to drop " + tableName, e);
                            }
                        }
                );

        firstStartupInitializer.initRoot();
        firstStartupInitializer.initCurrencies();
    }

    @Override
    public long getRepeatTime() {
        return 24;
    }

    @Override
    public long getInitDelay() {
        return 24;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.HOURS;
    }

    @Override
    public String name() {
        return "Demo";
    }
}

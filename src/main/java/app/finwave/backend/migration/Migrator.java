package app.finwave.backend.migration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.DatabaseConfig;

@Singleton
public class Migrator {
    protected static final Logger log = LoggerFactory.getLogger(Migrator.class);

    protected DatabaseConfig config;
    protected Flyway flyway;

    @Inject
    public Migrator(Configs configs) {
        this.config = configs.getState(new DatabaseConfig());
    }

    public void migrate() throws Exception {
        flyway = Flyway.configure()
                .dataSource(config.url, config.user, config.password)
                .baselineVersion("1.0.0")
                .validateMigrationNaming(true)
                .sqlMigrationSuffixes(".sql")
                .sqlMigrationPrefix("V")
                .sqlMigrationSeparator("__")
                .loggers("slf4j")
                .load();

        flyway.migrate();
    }
}

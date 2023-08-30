package su.knst.finwave.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.knst.finwave.config.Configs;
import su.knst.finwave.config.general.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;

@Singleton
public class Database {
    protected static final Logger log = LoggerFactory.getLogger(Database.class);
    protected DatabaseConfig config;

    protected Flyway flyway;
    protected Connection connection;
    protected DSLContext context;

    @Inject
    public Database(Configs configs) {
        config = configs.getState(new DatabaseConfig());

        log.info("Init database...");

        System.setProperty("org.jooq.no-logo", "true");

        flyway = Flyway.configure()
                .dataSource(config.url, config.user, config.password)
                .baselineVersion("1.0.0")
                .validateMigrationNaming(false) // TODO: Flyaway does not accept any migration name at runtime
                .loggers("slf4j")
                .load();

        try {
            flyway.migrate();
        } catch (Exception e) {
            log.error("Error to migrate", e);

            System.exit(1);
        }

        log.info("Connect to database...");

        try {
            connection = DriverManager.getConnection(config.url, config.user, config.password);

            context = DSL.using(connection, SQLDialect.POSTGRES);
        } catch (Exception e) {
            log.error("Error to connect", e);

            System.exit(1);
        }
    }

    public DSLContext context() {
        return context;
    }
}

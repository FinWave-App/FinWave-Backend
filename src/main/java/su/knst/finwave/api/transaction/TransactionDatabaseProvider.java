package su.knst.finwave.api.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import su.knst.finwave.api.transaction.metadata.MetadataDatabase;
import su.knst.finwave.database.Database;

@Singleton
public class TransactionDatabaseProvider {
    protected TransactionDatabase defaultDatabase;

    @Inject
    public TransactionDatabaseProvider(Database database) {
        this.defaultDatabase = new TransactionDatabase(database.context());
    }

    public TransactionDatabase get(DSLContext context) {
        return context == null ? defaultDatabase : new TransactionDatabase(context);
    }

    public TransactionDatabase get() {
        return defaultDatabase;
    }
}

package app.finwave.backend.database;

import org.jooq.DSLContext;

public abstract class AbstractDatabase {
    protected DSLContext context;

    public AbstractDatabase(DSLContext context) {
        this.context = context;
    }

}

package su.knst.finwave.api.transaction.metadata;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record1;
import su.knst.finwave.database.Database;

import static su.knst.finwave.jooq.Tables.*;

@Singleton
public class MetadataDatabase {
    protected DSLContext context;

    @Inject
    public MetadataDatabase(Database database) {
        this.context = database.context();
    }

    public long createMetadata(MetadataType type, long arg) {
        return context.insertInto(TRANSACTIONS_METADATA)
                .set(TRANSACTIONS_METADATA.TYPE, type.type)
                .set(TRANSACTIONS_METADATA.ARG, arg)
                .returningResult(TRANSACTIONS_METADATA.ID)
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();
    }

    public void deleteMetadata(long id) {
        context.deleteFrom(TRANSACTIONS_METADATA)
                .where(TRANSACTIONS_METADATA.ID.eq(id))
                .execute();
    }

    public long createInternalTransferMetadata(long fromTransactionId, long toTransactionId) {
        long internalId = context.insertInto(INTERNAL_TRANSFERS)
                .set(INTERNAL_TRANSFERS.FROM_TRANSACTION_ID, fromTransactionId)
                .set(INTERNAL_TRANSFERS.TO_TRANSACTION_ID, toTransactionId)
                .returningResult(INTERNAL_TRANSFERS.ID)
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        return createMetadata(MetadataType.INTERNAL_TRANSFER, internalId);
    }
}

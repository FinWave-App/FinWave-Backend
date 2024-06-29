package app.finwave.backend.api.transaction.metadata;

import org.jooq.DSLContext;
import org.jooq.Record1;
import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.InternalTransactionsMetadataRecord;

import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class MetadataDatabase extends AbstractDatabase {
    public MetadataDatabase(DSLContext context) {
        super(context);
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

    public long createInternalMetadata(long fromTransactionId, long toTransactionId) {
        long internalId = context.insertInto(INTERNAL_TRANSACTIONS_METADATA)
                .set(INTERNAL_TRANSACTIONS_METADATA.FROM_TRANSACTION_ID, fromTransactionId)
                .set(INTERNAL_TRANSACTIONS_METADATA.TO_TRANSACTION_ID, toTransactionId)
                .returningResult(INTERNAL_TRANSACTIONS_METADATA.ID)
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        return createMetadata(MetadataType.INTERNAL_TRANSFER, internalId);
    }

    public Optional<InternalTransactionsMetadataRecord> getInternalMetadata(long id) {
        return context.selectFrom(INTERNAL_TRANSACTIONS_METADATA)
                .where(INTERNAL_TRANSACTIONS_METADATA.ID.eq(id))
                .fetchOptional();
    }
}

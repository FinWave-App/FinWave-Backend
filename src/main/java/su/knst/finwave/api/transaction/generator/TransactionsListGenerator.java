package su.knst.finwave.api.transaction.generator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.Record;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.metadata.MetadataDatabase;
import su.knst.finwave.api.transaction.metadata.MetadataType;
import su.knst.finwave.jooq.tables.records.InternalTransfersRecord;

import java.util.*;
import java.util.function.Function;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS;
import static su.knst.finwave.jooq.Tables.TRANSACTIONS_METADATA;

@Singleton
public class TransactionsListGenerator {
    protected TransactionDatabase database;
    protected MetadataDatabase metadataDatabase;
    protected HashMap<MetadataType, PrepareHandler> metadataHandlers = new HashMap<>();

    @Inject
    public TransactionsListGenerator(TransactionDatabase database, MetadataDatabase metadataDatabase) {
        this.database = database;
        this.metadataDatabase = metadataDatabase;

        metadataHandlers.put(MetadataType.WITHOUT_METADATA, this::prepareCommonTransaction);
        metadataHandlers.put(MetadataType.INTERNAL_TRANSFER, this::prepareInternalTransfer);
    }

    public ArrayList<TransactionEntry<?>> prepareTransactions(List<Record> transactions) {
        ArrayList<TransactionEntry<?>> result = new ArrayList<>();
        HashMap<Long, TransactionEntry<?>> addedTransactions = new HashMap<>();

        for (Record record : transactions) {
            MetadataType metadataType = Optional.ofNullable(record.get(TRANSACTIONS_METADATA.TYPE))
                    .map(MetadataType::get)
                    .orElse(MetadataType.WITHOUT_METADATA);

            TransactionEntry<?> entry = metadataHandlers.get(metadataType).prepare(record, addedTransactions);

            if (entry != null) {
                result.add(entry);
                addedTransactions.put(entry.transactionId, entry);
            }
        }

        return result;
    }

    protected TransactionEntry<?> prepareCommonTransaction(Record record, HashMap<Long, TransactionEntry<?>> added) {
        return new TransactionEntry<>(record, null);
    }

    protected TransactionEntry<?> prepareInternalTransfer(Record record, HashMap<Long, TransactionEntry<?>> added) {
        long metadataId = record.get(TRANSACTIONS.METADATA_ID);
        boolean linkedInResult = added
                .values()
                .stream()
                .anyMatch((t) ->
                        t.metadata instanceof InternalTransferMetadata meta &&
                        meta.id == metadataId);

        if (linkedInResult)
            return null;

        InternalTransfersRecord metaRecord = metadataDatabase.getInternalTransferMeta(metadataId).orElseThrow();

        long toFetch = record.get(TRANSACTIONS.DELTA).signum() > 0 ? metaRecord.getFromTransactionId() : metaRecord.getToTransactionId();
        TransactionEntry<?> secondTransaction = database.getTransaction(toFetch).map(TransactionEntry::new).orElseThrow();

        return new TransactionEntry<>(record, new InternalTransferMetadata(metadataId, secondTransaction));
    }
}

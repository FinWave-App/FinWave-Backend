package su.knst.finwave.api.transaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.*;
import org.jooq.Record;
import su.knst.finwave.api.transaction.filter.TransactionsFilter;
import su.knst.finwave.api.transaction.metadata.MetadataDatabase;
import su.knst.finwave.api.transaction.metadata.MetadataType;
import su.knst.finwave.database.Database;
import su.knst.finwave.jooq.tables.records.InternalTransfersRecord;
import su.knst.finwave.jooq.tables.records.TransactionsMetadataRecord;
import su.knst.finwave.jooq.tables.records.TransactionsRecord;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.*;

@Singleton
public class TransactionDatabase {

    protected DSLContext context;

    protected MetadataDatabase metadataDatabase;

    @Inject
    public TransactionDatabase(Database database, MetadataDatabase metadataDatabase) {
        this.context = database.context();
        this.metadataDatabase = metadataDatabase;
    }

    public long applyInternalTransfer(int userId, long tagId, long fromAccountId, long toAccountId, OffsetDateTime created, BigDecimal fromDelta, BigDecimal toDelta, String description) {
        long fromTransaction = applyTransaction(userId, tagId, fromAccountId, created, fromDelta, description);
        long toTransaction = applyTransaction(userId, tagId, toAccountId, created, toDelta, description);

        long metadata = metadataDatabase.createInternalTransferMetadata(fromTransaction, toTransaction);

        context.update(TRANSACTIONS)
                .set(TRANSACTIONS.METADATA_ID, metadata)
                .where(TRANSACTIONS.ID.eq(fromTransaction).or(TRANSACTIONS.ID.eq(toTransaction)))
                .execute();

        return fromTransaction;
    }

    public long applyTransaction(int userId, long tagId, long accountId, OffsetDateTime created, BigDecimal delta, String description) {
        return context.transactionResult((configuration) -> {
            DSLContext transaction = configuration.dsl();

            Field<Long> currencyId = transaction.select(ACCOUNTS.CURRENCY_ID)
                    .from(ACCOUNTS)
                    .where(ACCOUNTS.ID.eq(accountId))
                    .asField();

            Optional<Long> record = transaction.insertInto(TRANSACTIONS)
                    .set(TRANSACTIONS.OWNER_ID, userId)
                    .set(TRANSACTIONS.TAG_ID, tagId)
                    .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                    .set(TRANSACTIONS.CURRENCY_ID, currencyId)
                    .set(TRANSACTIONS.CREATED_AT, created)
                    .set(TRANSACTIONS.DELTA, delta)
                    .set(TRANSACTIONS.DESCRIPTION, description)
                    .returningResult(TRANSACTIONS.ID)
                    .fetchOptional()
                    .map(Record1::component1);

            if (record.isEmpty())
                throw new RuntimeException("Fail to create new transaction");

            transaction.update(ACCOUNTS)
                    .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
                    .where(ACCOUNTS.ID.eq(accountId))
                    .execute();

            return record.get();
        });
    }

    private void cancelTransaction(DSLContext context, TransactionsRecord record) {
        context.deleteFrom(TRANSACTIONS)
                .where(TRANSACTIONS.ID.eq(record.getId()))
                .execute();

        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.getDelta()))
                .where(ACCOUNTS.ID.eq(record.getAccountId()))
                .execute();
    }

    private void cancelTransactionWithMeta(DSLContext context, TransactionsRecord record) {
        TransactionsMetadataRecord metadataRecord = context
                .selectFrom(TRANSACTIONS_METADATA)
                .where(TRANSACTIONS_METADATA.ID.eq(record.getMetadataId()))
                .fetchOne();

        if (metadataRecord == null)
            throw new RuntimeException("Metadata not exists");

        MetadataType type = MetadataType.get(metadataRecord.getType());

        if (type == MetadataType.INTERNAL_TRANSFER) {
            InternalTransfersRecord internalTransfersRecord = context.selectFrom(INTERNAL_TRANSFERS)
                    .where(INTERNAL_TRANSFERS.ID.eq(metadataRecord.getArg()))
                    .fetchOptional()
                    .orElseThrow();

            long toFetch = record.getId().equals(internalTransfersRecord.getFromTransactionId()) ?
                    internalTransfersRecord.getToTransactionId() :
                    internalTransfersRecord.getFromTransactionId();

            TransactionsRecord record2 = context.selectFrom(TRANSACTIONS)
                    .where(TRANSACTIONS.ID.eq(toFetch))
                    .fetchOne();

            if (record2 == null)
                throw new RuntimeException("Second transaction not exists");

            context.deleteFrom(INTERNAL_TRANSFERS)
                    .where(INTERNAL_TRANSFERS.ID.eq(internalTransfersRecord.getId()))
                    .execute();

            cancelTransaction(context, record);
            cancelTransaction(context, record2);

            context.deleteFrom(TRANSACTIONS_METADATA)
                    .where(TRANSACTIONS_METADATA.ID.eq(metadataRecord.getId()))
                    .execute();
        }
    }

    public void cancelTransaction(long transactionId) {
        context.transaction((configuration) -> {
            DSLContext transaction = configuration.dsl();

            Optional<TransactionsRecord> record = transaction.selectFrom(TRANSACTIONS)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .fetchOptional();

            if (record.isEmpty())
                throw new RuntimeException("Transaction not exists");

            if (record.get().getMetadataId() == null) {
                cancelTransaction(transaction, record.get());
            }else {
                cancelTransactionWithMeta(transaction, record.get());
            }
        });
    }

    public int getTransactionsCount(int userId, TransactionsFilter filter) {
        Condition condition = generateFilterCondition(userId, filter);

        return context.selectCount()
                .from(TRANSACTIONS)
                .where(condition)
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public Optional<Record> getTransaction(long id) {
        return context.selectFrom(TRANSACTIONS
                        .leftJoin(TRANSACTIONS_METADATA)
                        .on(TRANSACTIONS.METADATA_ID.eq(TRANSACTIONS_METADATA.ID)))
                .where(TRANSACTIONS.ID.eq(id))
                .fetchOptional();
    }

    public List<Record> getTransactions(int userId, int offset, int count, TransactionsFilter filter) {
        Condition condition = generateFilterCondition(userId, filter);

        return context.selectFrom(TRANSACTIONS
                        .leftJoin(TRANSACTIONS_METADATA)
                        .on(TRANSACTIONS.METADATA_ID.eq(TRANSACTIONS_METADATA.ID)))
                .where(condition)
                .orderBy(TRANSACTIONS.CREATED_AT.desc(), TRANSACTIONS.ID.desc())
                .limit(offset, count)
                .fetch();
    }

    public Condition generateFilterCondition(int userId, TransactionsFilter filter) {
        Condition condition = TRANSACTIONS.OWNER_ID.eq(userId);

        if (filter.getTagsIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.TAG_ID, filter.getTagsIds()));

        if (filter.getAccountIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.ACCOUNT_ID, filter.getAccountIds()));

        if (filter.getCurrenciesIds() != null)
            condition = condition.and(generateFilterAnyCondition(TRANSACTIONS.CURRENCY_ID, filter.getCurrenciesIds()));

        if (filter.getFromTime() != null)
            condition = condition.and(TRANSACTIONS.CREATED_AT.greaterOrEqual(filter.getFromTime()));

        if (filter.getToTime() != null)
            condition = condition.and(TRANSACTIONS.CREATED_AT.lessOrEqual(filter.getToTime()));

        if (filter.getDescriptionContains() != null)
            condition = condition.and(TRANSACTIONS.DESCRIPTION.containsIgnoreCase(filter.getDescriptionContains()));

        return condition;
    }

    protected <T> Condition generateFilterAnyCondition(TableField<?, T> field, List<T> values) {
        Condition condition = null;

        for (T value : values) {
            if (condition == null) {
                condition = field.eq(value);

                continue;
            }

            condition = condition.or(field.eq(value));
        }

        return condition;
    }

    public void editTransaction(long transactionId, long tagId, long accountId, OffsetDateTime created, BigDecimal delta, String description) {
        context.transaction((configuration) -> {
            DSLContext transaction = configuration.dsl();

            Optional<TransactionsRecord> record = transaction.selectFrom(TRANSACTIONS)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .fetchOptional();

            if (record.isEmpty())
                throw new RuntimeException("Transaction not exists");

            transaction.update(ACCOUNTS)
                    .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.get().getDelta()))
                    .where(ACCOUNTS.ID.eq(record.get().getAccountId()))
                    .execute();

            Optional<Long> currencyId = transaction
                    .update(ACCOUNTS)
                    .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
                    .where(ACCOUNTS.ID.eq(accountId))
                    .returningResult(ACCOUNTS.CURRENCY_ID)
                    .fetchOptional()
                    .map(Record1::component1);

            if (currencyId.isEmpty())
                throw new RuntimeException("Failed to get currency id and modify account");

            transaction.update(TRANSACTIONS)
                    .set(TRANSACTIONS.TAG_ID, tagId)
                    .set(TRANSACTIONS.ACCOUNT_ID, accountId)
                    .set(TRANSACTIONS.CURRENCY_ID, currencyId.get())
                    .set(TRANSACTIONS.CREATED_AT, created)
                    .set(TRANSACTIONS.DELTA, delta)
                    .set(TRANSACTIONS.DESCRIPTION, description)
                    .where(TRANSACTIONS.ID.eq(transactionId))
                    .execute();
        });
    }

    public boolean userOwnTransaction(int userId, long transactionId) {
        return context.select(TRANSACTIONS.ID)
                .from(TRANSACTIONS)
                .where(TRANSACTIONS.OWNER_ID.eq(userId).and(TRANSACTIONS.ID.eq(transactionId)))
                .fetchOptional()
                .isPresent();
    }
}

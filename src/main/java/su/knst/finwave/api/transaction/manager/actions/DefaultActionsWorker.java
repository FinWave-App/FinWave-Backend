package su.knst.finwave.api.transaction.manager.actions;

import org.checkerframework.checker.units.qual.A;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import su.knst.finwave.api.transaction.TransactionDatabase;
import su.knst.finwave.api.transaction.manager.generator.AbstractMetadata;
import su.knst.finwave.api.transaction.manager.generator.TransactionEntry;
import su.knst.finwave.api.transaction.manager.records.TransactionEditRecord;
import su.knst.finwave.api.transaction.manager.records.TransactionNewRecord;

import java.util.HashMap;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.ACCOUNTS;
import static su.knst.finwave.jooq.Tables.TRANSACTIONS;

public class DefaultActionsWorker implements TransactionActionsWorker<TransactionNewRecord, TransactionEditRecord, AbstractMetadata> {
    @Override
    public long apply(DSLContext context, TransactionDatabase database, TransactionNewRecord newRecord) {
        Long currencyId = context.select(ACCOUNTS.CURRENCY_ID)
                .from(ACCOUNTS)
                .where(ACCOUNTS.ID.eq(newRecord.accountId()))
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        Optional<Long> transactionId = database.applyTransaction(
                newRecord.userId(),
                newRecord.tagId(),
                newRecord.accountId(),
                currencyId,
                newRecord.created(),
                newRecord.delta(),
                newRecord.description()
        );

        if (transactionId.isEmpty())
            throw new RuntimeException("Fail to create new transaction");

        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(newRecord.delta()))
                .where(ACCOUNTS.ID.eq(newRecord.accountId()))
                .execute();

        return transactionId.get();
    }

    @Override
    public void edit(DSLContext context, TransactionDatabase database, Record record, TransactionEditRecord newRecord) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.get(TRANSACTIONS.DELTA)))
                .where(ACCOUNTS.ID.eq(record.get(TRANSACTIONS.ACCOUNT_ID)))
                .execute();

        Optional<Long> currencyId = context
                .update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(newRecord.delta()))
                .where(ACCOUNTS.ID.eq(newRecord.accountId()))
                .returningResult(ACCOUNTS.CURRENCY_ID)
                .fetchOptional()
                .map(Record1::component1);

        if (currencyId.isEmpty())
            throw new RuntimeException("Failed to get currency id and modify account");

        database.editTransaction(record.get(TRANSACTIONS.ID), newRecord.tagId(), newRecord.accountId(), currencyId.get(), newRecord.created(), newRecord.delta(), newRecord.description());
    }

    @Override
    public void cancel(DSLContext context, TransactionDatabase database, Record record) {
        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.get(TRANSACTIONS.DELTA)))
                .where(ACCOUNTS.ID.eq(record.get(TRANSACTIONS.ACCOUNT_ID)))
                .execute();

        database.deleteTransaction(record.get(TRANSACTIONS.ID));
    }

    @Override
    public TransactionEntry<AbstractMetadata> prepareEntry(DSLContext context, TransactionDatabase database, Record record, HashMap<Long, TransactionEntry<?>> added) {
        return new TransactionEntry<>(record, null);
    }
}
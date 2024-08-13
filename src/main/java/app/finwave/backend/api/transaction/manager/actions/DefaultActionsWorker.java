package app.finwave.backend.api.transaction.manager.actions;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import app.finwave.backend.api.transaction.TransactionDatabase;
import app.finwave.backend.api.transaction.manager.data.AbstractMetadata;
import app.finwave.backend.api.transaction.manager.data.TransactionEntry;
import app.finwave.backend.api.transaction.manager.records.TransactionEditRecord;
import app.finwave.backend.api.transaction.manager.records.TransactionNewRecord;
import app.finwave.backend.database.DatabaseWorker;

import java.util.HashMap;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.ACCOUNTS;
import static app.finwave.backend.jooq.Tables.TRANSACTIONS;

public class DefaultActionsWorker extends TransactionActionsWorker<TransactionNewRecord, TransactionEditRecord, AbstractMetadata> {
    public DefaultActionsWorker(DatabaseWorker databaseWorker) {
        super(databaseWorker);
    }

    @Override
    public long apply(DSLContext context, TransactionNewRecord newRecord) {
        TransactionDatabase database = databaseWorker.get(TransactionDatabase.class, context);

        Long currencyId = context.select(ACCOUNTS.CURRENCY_ID)
                .from(ACCOUNTS)
                .where(ACCOUNTS.ID.eq(newRecord.accountId()))
                .fetchOptional()
                .map(Record1::component1)
                .orElseThrow();

        Optional<Long> transactionId = database.applyTransaction(
                newRecord.userId(),
                newRecord.categoryId(),
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
    public void edit(DSLContext context, Record record, TransactionEditRecord editRecord) {
        TransactionDatabase database = databaseWorker.get(TransactionDatabase.class, context);

        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.get(TRANSACTIONS.DELTA)))
                .where(ACCOUNTS.ID.eq(record.get(TRANSACTIONS.ACCOUNT_ID)))
                .execute();

        Optional<Long> currencyId = context
                .update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(editRecord.delta()))
                .where(ACCOUNTS.ID.eq(editRecord.accountId()))
                .returningResult(ACCOUNTS.CURRENCY_ID)
                .fetchOptional()
                .map(Record1::component1);

        if (currencyId.isEmpty())
            throw new RuntimeException("Failed to get currency id and modify account");

        database.editTransaction(record.get(TRANSACTIONS.ID), editRecord.categoryId(), editRecord.accountId(), currencyId.get(), editRecord.created(), editRecord.delta(), editRecord.description());
    }

    @Override
    public void cancel(DSLContext context, Record record) {
        TransactionDatabase database = databaseWorker.get(TransactionDatabase.class, context);

        context.update(ACCOUNTS)
                .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(record.get(TRANSACTIONS.DELTA)))
                .where(ACCOUNTS.ID.eq(record.get(TRANSACTIONS.ACCOUNT_ID)))
                .execute();

        database.deleteTransaction(record.get(TRANSACTIONS.ID));
    }

    @Override
    public TransactionEntry<AbstractMetadata> prepareEntry(DSLContext context, Record record, HashMap<Long, TransactionEntry<?>> added) {
        return new TransactionEntry<>(record, null);
    }
}

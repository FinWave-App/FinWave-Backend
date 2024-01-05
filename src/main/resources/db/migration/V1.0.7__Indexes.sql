create index idx_transactions on transactions(id, owner_id, tag_id, account_id, currency_id, created_at, delta, description, metadata_id);

create index idx_transactions_metadata on transactions_metadata(id);
create index idx_internal_transactions_metadata on internal_transactions_metadata(id, from_transaction_id, to_transaction_id);

create index idx_users_sessions on users_sessions(token);
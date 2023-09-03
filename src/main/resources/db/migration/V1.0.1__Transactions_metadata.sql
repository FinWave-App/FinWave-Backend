create table transactions_metadata
(
    id                    bigserial primary key,
    type                  smallint not null,
    arg                   bigint
);

alter table transactions
    add metadata_id bigint references transactions_metadata(id);

create table internal_transfers
(
    id                    bigserial primary key,
    from_transaction_id   bigint not null references transactions(id),
    to_transaction_id     bigint not null references transactions(id)
);
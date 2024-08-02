create table if not exists transactions_tags_management
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    tag_id                bigint not null references transactions_tags(id),
    date_type             smallint not null,
    currency_id           bigint not null references currencies(id),
    amount                numeric not null
);

create index idx_transactions_tags_management on transactions_tags_management(tag_id, owner_id);
create table accumulation_settings
(
    source_account_id     bigint not null references accounts(id) unique,
    target_account_id     bigint not null references accounts(id),
    tag_id                bigint not null references transactions_tags(id),
    owner_id              integer not null references users(id),
    steps                 jsonb not null
);
create extension ltree;

create table if not exists users
(
    id            serial primary key,
    username      text not null unique,
    password      text not null
);

alter table users
    owner to finwave;


create table if not exists users_sessions
(
    id            bigserial primary key,
    user_id       integer not null references users(id),
    token         text not null unique,
    created_at    timestamp not null,
    expires_at    timestamp not null,
    description   text
);

alter table users_sessions
    owner to finwave;


create table if not exists users_settings
(
    id            serial primary key,
    user_id       integer not null references users(id),
    language      VARCHAR(16) not null,
    time_zone     text not null
);

alter table users_settings
    owner to finwave;


create table if not exists notes
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    notification_time     timestamp with time zone,
    last_edit             timestamp with time zone not null,
    note                  text not null
);

alter table notes
    owner to finwave;


create table if not exists accounts_tags
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    name                  text not null,
    description           text
);

alter table accounts_tags
    owner to finwave;


create table if not exists transactions_tags
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    type                  smallint not null,
    parents_tree          ltree not null,
    name                  text not null,
    description           text
);

alter table transactions_tags
    owner to finwave;


create table if not exists currencies
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    code                  text not null,
    symbol                text not null,
    decimals              smallint not null,
    description           text not null
);

alter table currencies
    owner to finwave;


create table if not exists accounts
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    tag_id                bigint not null references accounts_tags(id),
    currency_id           bigint not null references currencies(id),
    amount                numeric not null,
    hidden                boolean not null,
    name                  text not null,
    description           text
 );

alter table accounts
    owner to finwave;


create table if not exists transactions
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    tag_id                bigint not null references transactions_tags(id),
    account_id            bigint not null references accounts(id),
    currency_id           bigint not null references currencies(id),
    created_at            timestamp with time zone not null,
    delta                 numeric not null,
    description           text
);

alter table transactions
    owner to finwave;


create table if not exists recurring_transactions
(
    id                    bigserial primary key,
    owner_id              integer not null references users(id),
    tag_id                bigint not null references transactions_tags(id),
    account_id            bigint not null references accounts(id),
    currency_id           bigint not null references currencies(id),
    last_repeat           timestamp with time zone not null,
    next_repeat           timestamp with time zone not null,
    repeat_func           smallint not null,
    repeat_func_arg       smallint not null,
    notification_mode     smallint not null,
    delta                 numeric not null,
    description           text
);

alter table transactions
    owner to finwave;
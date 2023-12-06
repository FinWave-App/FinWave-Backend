create table notifications_pull
(
    id                    bigserial primary key,
    text                  text not null,
    options               jsonb not null,
    user_id               integer not null references users(id),
    created_at            timestamp with time zone not null
);

create table notifications_points
(
    id                    bigserial primary key,
    is_primary            boolean not null,
    user_id               integer not null references users(id),
    type                  smallint not null,
    created_at            timestamp with time zone not null,
    data                  jsonb not null,
    description           text not null
);


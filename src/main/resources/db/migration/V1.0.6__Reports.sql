create table reports
(
    id                    text not null unique,
    description           text,
    status                smallint not null,
    type                  smallint not null,
    filter                jsonb,
    lang                  jsonb,
    user_id               integer not null references users(id),
    created_at            timestamp with time zone not null,
    expires_at            timestamp with time zone not null
);

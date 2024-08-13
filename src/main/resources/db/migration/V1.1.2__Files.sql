create table if not exists files
(
    id                          text not null unique,
    owner_id                    integer not null references users(id),
    created_at                  timestamp with time zone not null,
    expires_at                  timestamp with time zone not null,
    is_public                   boolean not null,
    source                      text not null,

    size                        bigint,
    mime_type                   text,
    name                        text,
    description                 text,
    checksum                    text
);

create index idx_files on files(owner_id, created_at, expires_at);
create index idx_files_id on files using hash (id);
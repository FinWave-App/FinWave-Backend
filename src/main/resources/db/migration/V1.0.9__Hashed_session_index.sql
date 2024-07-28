drop index idx_users_sessions;

create index idx_users_sessions on users_sessions using hash (token);
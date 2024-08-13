create table if not exists ai_contexts
(
    id                          bigserial primary key,
    owner_id                    integer not null references users(id),
    created_at                  timestamp with time zone not null,
    completion_tokens_used      integer not null,
    prompt_tokens_used          integer not null
);

create table if not exists ai_messages
(
    id          bigserial primary key,
    ai_context  bigint not null references ai_contexts(id),
    role        text not null,
    content     json not null
);

create index idx_ai_contexts on ai_contexts(id, owner_id);

create index idx_ai_messages on ai_messages(id, ai_context);
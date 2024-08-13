truncate table reports;

alter table reports
    drop column id,
    drop column created_at,
    drop column expires_at;

alter table reports add column file_id text not null;
alter table reports add column id bigserial primary key;
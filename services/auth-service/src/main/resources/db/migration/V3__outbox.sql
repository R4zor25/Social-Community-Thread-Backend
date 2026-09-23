create table outbox (
    id           uuid primary key,
    aggregate_id varchar(64)  not null,
    type         varchar(100) not null,
    payload      jsonb        not null,
    created_at   timestamptz  not null,
    published_at timestamptz
);

create index outbox_unpublished_idx on outbox (created_at) where published_at is null;

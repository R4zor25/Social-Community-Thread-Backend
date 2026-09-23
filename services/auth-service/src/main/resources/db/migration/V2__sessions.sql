create table sessions (
    id         uuid primary key,
    user_id    bigint      not null references users (id) on delete cascade,
    created_at timestamptz not null,
    revoked_at timestamptz
);

create index sessions_user_id_idx on sessions (user_id);

create table refresh_tokens (
    token_hash varchar(64) primary key,
    session_id uuid        not null references sessions (id) on delete cascade,
    expires_at timestamptz not null,
    used_at    timestamptz
);

create index refresh_tokens_session_id_idx on refresh_tokens (session_id);

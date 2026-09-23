create table users (
    id            bigint generated always as identity primary key,
    username      varchar(32)  not null,
    email         varchar(254) not null,
    password_hash varchar(100) not null,
    created_at    timestamptz  not null
);

create unique index users_username_key on users (lower(username));
create unique index users_email_key on users (lower(email));

create table user_avatars (
    user_id      bigint primary key references users (id) on delete cascade,
    content      bytea        not null,
    content_type varchar(100) not null
);

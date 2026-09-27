create table threads (
    id          bigint generated always as identity primary key,
    name        varchar(100)  not null,
    description varchar(2000) not null,
    creator_id  bigint        not null references user_projection (user_id),
    created_at  timestamptz   not null
);

create index threads_created_at_idx on threads (created_at desc);

create table thread_images (
    thread_id    bigint primary key references threads (id) on delete cascade,
    content      bytea        not null,
    content_type varchar(100) not null
);

create table thread_followers (
    thread_id   bigint      not null references threads (id) on delete cascade,
    user_id     bigint      not null references user_projection (user_id),
    followed_at timestamptz not null,
    primary key (thread_id, user_id)
);

create index thread_followers_user_id_idx on thread_followers (user_id);

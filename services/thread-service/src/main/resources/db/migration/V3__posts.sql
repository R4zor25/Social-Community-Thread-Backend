create table posts (
    id         bigint generated always as identity primary key,
    thread_id  bigint       not null references threads (id) on delete cascade,
    author_id  bigint       not null references user_projection (user_id),
    title      varchar(200) not null,
    body       text         not null,
    tags       text[]       not null,
    score      integer      not null default 0,
    created_at timestamptz  not null
);

create index posts_thread_id_created_at_idx on posts (thread_id, created_at desc);
create index posts_author_id_idx on posts (author_id);

create table post_attachments (
    post_id      bigint primary key references posts (id) on delete cascade,
    content      bytea        not null,
    content_type varchar(100) not null
);

create table post_votes (
    post_id   bigint   not null references posts (id) on delete cascade,
    user_id   bigint   not null references user_projection (user_id),
    direction smallint not null check (direction in (-1, 1)),
    primary key (post_id, user_id)
);

create index post_votes_user_id_idx on post_votes (user_id);

create table saved_posts (
    user_id  bigint      not null references user_projection (user_id),
    post_id  bigint      not null references posts (id) on delete cascade,
    saved_at timestamptz not null,
    primary key (user_id, post_id)
);

create table comments (
    id         bigint generated always as identity primary key,
    post_id    bigint      not null references posts (id) on delete cascade,
    author_id  bigint      not null references user_projection (user_id),
    body       text        not null,
    score      integer     not null default 0,
    created_at timestamptz not null
);

create index comments_post_id_created_at_idx on comments (post_id, created_at);

create table comment_votes (
    comment_id bigint   not null references comments (id) on delete cascade,
    user_id    bigint   not null references user_projection (user_id),
    direction  smallint not null check (direction in (-1, 1)),
    primary key (comment_id, user_id)
);

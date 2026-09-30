create table conversations (
    id              bigint generated always as identity primary key,
    name            varchar(100) not null,
    creator_id      bigint       not null references user_projection (user_id),
    created_at      timestamptz  not null,
    last_message_at timestamptz  not null
);

create table conversation_images (
    conversation_id bigint primary key references conversations (id) on delete cascade,
    content         bytea        not null,
    content_type    varchar(100) not null
);

create table conversation_participants (
    conversation_id bigint      not null references conversations (id) on delete cascade,
    user_id         bigint      not null references user_projection (user_id),
    joined_at       timestamptz not null,
    primary key (conversation_id, user_id)
);

create index conversation_participants_user_idx on conversation_participants (user_id);

create table messages (
    id              bigint generated always as identity primary key,
    conversation_id bigint      not null references conversations (id) on delete cascade,
    author_id       bigint      not null references user_projection (user_id),
    body            text        not null,
    sent_at         timestamptz not null
);

create index messages_conversation_sent_at_idx on messages (conversation_id, sent_at desc);

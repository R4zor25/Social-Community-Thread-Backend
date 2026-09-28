create table friend_requests (
    id           bigint generated always as identity primary key,
    sender_id    bigint      not null references user_projection (user_id),
    recipient_id bigint      not null references user_projection (user_id),
    created_at   timestamptz not null,
    check (sender_id <> recipient_id)
);

-- One pending request per pair of users, whichever of them sent it; this also settles simultaneous mutual requests.
create unique index friend_requests_pair_key on friend_requests (least(sender_id, recipient_id), greatest(sender_id, recipient_id));
create index friend_requests_recipient_idx on friend_requests (recipient_id, created_at desc);
create index friend_requests_sender_idx on friend_requests (sender_id, created_at desc);

create table friendships (
    user_low   bigint      not null references user_projection (user_id),
    user_high  bigint      not null references user_projection (user_id),
    created_at timestamptz not null,
    primary key (user_low, user_high),
    check (user_low < user_high)
);

create index friendships_user_high_idx on friendships (user_high);

-- Users as far as a consuming service needs them, filled from auth-service's user-events.
-- Shipped by libs/user-projection; services number their own migrations from V2.
create table user_projection (
    user_id    bigint primary key,
    username   varchar(32) not null,
    updated_at timestamptz not null
);

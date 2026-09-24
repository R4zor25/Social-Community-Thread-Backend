#!/bin/bash
# Runs once on an empty data directory: one database and one owner per service, no access to the others.
set -euo pipefail

create_service_database() {
  local name="$1" password="$2"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v name="$name" -v password="$password" <<'SQL'
CREATE USER :"name" WITH PASSWORD :'password';
CREATE DATABASE :"name" OWNER :"name";
REVOKE CONNECT ON DATABASE :"name" FROM PUBLIC;
SQL
}

create_service_database auth "$AUTH_DB_PASSWORD"
create_service_database thread "$THREAD_DB_PASSWORD"
create_service_database friend "$FRIEND_DB_PASSWORD"
create_service_database chat "$CHAT_DB_PASSWORD"

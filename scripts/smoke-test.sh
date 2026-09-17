#!/usr/bin/env bash
# End-to-end check against a running stack: docker compose up --build, then ./scripts/smoke-test.sh
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8765}"
SUFFIX="$(date +%s)"
FAILURES=0

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

expect() {
  local description="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "ok    $description ($actual)"
  else
    echo "FAIL  $description: expected $expected, got $actual"
    FAILURES=$((FAILURES + 1))
  fi
}

json_field() { sed -n "s/.*\"$1\":\"\{0,1\}\([^,\"}]*\).*/\1/p"; }

register() {
  status -X POST "$GATEWAY/api/auth/register" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"secret\",\"email\":\"$1@example.com\"}"
}

login() {
  curl -s -X POST "$GATEWAY/api/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"secret\"}"
}

echo "Waiting for the gateway to route to auth-service..."
for _ in $(seq 1 60); do
  code="$(status -X POST "$GATEWAY/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"-","password":"-"}' || true)"
  [[ "$code" == "403" ]] && break
  sleep 2
done

alice="alice$SUFFIX"
bob="bob$SUFFIX"
expect "register $alice" 200 "$(register "$alice")"
expect "register $bob" 200 "$(register "$bob")"

alice_login="$(login "$alice")"
token="$(echo "$alice_login" | json_field accessToken)"
alice_id="$(echo "$alice_login" | json_field userId)"
bob_id="$(login "$bob" | json_field userId)"
[[ -n "$token" && -n "$alice_id" && -n "$bob_id" ]] || { echo "FAIL  could not log in"; exit 1; }

expect "own data with token" 200 "$(status -H "Authorization: Bearer $token" "$GATEWAY/api/thread/$alice_id/saved")"
expect "own data without token" 401 "$(status "$GATEWAY/api/thread/$alice_id/saved")"
expect "another user's data" 403 "$(status -H "Authorization: Bearer $token" "$GATEWAY/api/thread/$bob_id/saved")"
expect "another user's data with a spoofed X-User-Id" 403 \
  "$(status -H "Authorization: Bearer $token" -H "X-User-Id: $bob_id" "$GATEWAY/api/thread/$bob_id/saved")"

if [[ "$FAILURES" -gt 0 ]]; then
  echo "$FAILURES check(s) failed"
  exit 1
fi
echo "All checks passed"

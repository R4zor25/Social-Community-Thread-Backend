#!/usr/bin/env bash
# End-to-end checks through the gateway against a running stack:
#   scripts/generate-keys.sh && docker compose up --build --wait && scripts/smoke-test.sh
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
API="$GATEWAY/api/v2"
SUFFIX="$(date +%s)$RANDOM"
FAILURES=0
BODY="$(mktemp)"
trap 'rm -f "$BODY"' EXIT

# Prints the status code; the response body is left in $BODY.
request() { curl -s -o "$BODY" -w '%{http_code}' "$@"; }
json() { curl -s -H 'Content-Type: application/json' "$@"; }
# The first occurrence of a top-level-looking field; good enough for the flat checks below.
field() { grep -o "\"$1\":\"\{0,1\}[^,\"}]*" "$BODY" | head -1 | sed "s/^\"$1\":\"\{0,1\}//"; }

expect() {
  local description="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "ok    $description ($actual)"
  else
    echo "FAIL  $description: expected $expected, got $actual"
    FAILURES=$((FAILURES + 1))
  fi
}

# Waits until a route answers with something other than a gateway or startup error.
wait_for() {
  local description="$1"; shift
  for _ in $(seq 1 60); do
    case "$(request "$@" || true)" in 000|502|503|504) sleep 2 ;; *) return 0 ;; esac
  done
  echo "FAIL  $description did not become reachable"
  exit 1
}

register() {
  request -X POST "$API/auth/register" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"email\":\"$1@example.com\",\"password\":\"correct horse\"}"
}

login() {
  request -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"correct horse\"}" > /dev/null
}

refresh() {
  request -X POST "$API/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$1\"}"
}

wait_for "auth-service through the gateway" -X POST "$API/auth/login" -H 'Content-Type: application/json' -d '{"username":"-","password":"-"}'

echo "-- auth"
alice="alice$SUFFIX"
bob="bob$SUFFIX"
expect "register $alice" 201 "$(register "$alice")"
expect "register $bob" 201 "$(register "$bob")"
bob_id="$(field id)"

login "$alice"
token="$(field accessToken)"
refresh_token="$(field refreshToken)"
[[ -n "$token" && -n "$refresh_token" ]] || { echo "FAIL  login returned no tokens"; exit 1; }
auth=(-H "Authorization: Bearer $token")

expect "own profile" 200 "$(request "${auth[@]}" "$API/users/me")"
expect "own profile shows the email" "$alice@example.com" "$(field email)"
expect "another user's profile" 200 "$(request "${auth[@]}" "$API/users/$bob_id")"
expect "another user's profile hides the email" "" "$(field email)"

expect "refresh" 200 "$(refresh "$refresh_token")"
rotated="$(field refreshToken)"
expect "reusing a rotated refresh token" 401 "$(refresh "$refresh_token")"
expect "reuse revoked the rotated token too" 401 "$(refresh "$rotated")"

login "$alice"
fresh="$(field refreshToken)"
expect "logout" 204 "$(request -X POST "$API/auth/logout" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$fresh\"}")"
expect "refresh after logout" 401 "$(refresh "$fresh")"

expect "no token" 401 "$(request "$API/users/me")"
# Change the first signature character: it carries 6 signature bits. The last one carries only 2 plus padding,
# so some replacements there decode to the same signature and the token stays valid.
signature="${token##*.}"; [[ "${signature:0:1}" == "A" ]] && other="B" || other="A"
expect "tampered token" 401 "$(request -H "Authorization: Bearer ${token%.*}.${other}${signature:1}" "$API/users/me")"
expect "key set is not routed" 404 "$(request "${auth[@]}" "$GATEWAY/.well-known/jwks.json")"

printf '\x89PNG\r\n\x1a\nsmoke' > "$BODY.png"
expect "avatar upload" 204 "$(request -X PUT "${auth[@]}" -H 'Content-Type: image/png' --data-binary "@$BODY.png" "$API/users/me/avatar")"
content_type="$(curl -s -o /dev/null -w '%{content_type}' "${auth[@]}" "$API/users/$(json "${auth[@]}" "$API/users/me" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')/avatar")"
expect "avatar content type" "image/png" "$content_type"
rm -f "$BODY.png"

echo "-- threads"
wait_for "thread-service through the gateway" "${auth[@]}" "$API/feed"
login "$bob"
bob_auth=(-H "Authorization: Bearer $(field accessToken)")

expect "create thread" 201 "$(request -X POST "${auth[@]}" -H 'Content-Type: application/json' -d '{"name":"Smoke","description":"Smoke test thread"}' "$API/threads")"
thread_id="$(field id)"
expect "bob follows the thread" 204 "$(request -X PUT "${bob_auth[@]}" "$API/threads/$thread_id/follow")"
expect "bob posts" 201 "$(request -X POST "${bob_auth[@]}" -H 'Content-Type: application/json' -d '{"title":"Hello","body":"From bob","tags":["smoke"]}' "$API/threads/$thread_id/posts")"
post_id="$(field id)"
expect "post shows the author name" "$bob" "$(sed -n 's/.*"author":{"id":[0-9]*,"username":"\([^"]*\)".*/\1/p' "$BODY")"
expect "alice comments" 201 "$(request -X POST "${auth[@]}" -H 'Content-Type: application/json' -d '{"body":"Welcome"}' "$API/posts/$post_id/comments")"
expect "alice votes" 204 "$(request -X PUT "${auth[@]}" -H 'Content-Type: application/json' -d '{"direction":"UP"}' "$API/posts/$post_id/vote")"
expect "alice saves" 204 "$(request -X PUT "${auth[@]}" "$API/posts/$post_id/save")"
expect "post details" 200 "$(request "${auth[@]}" "$API/posts/$post_id")"
expect "score after vote" 1 "$(field score)"
expect "bob's feed" 200 "$(request "${bob_auth[@]}" "$API/feed")"
expect "feed has the post" 1 "$(field totalItems)"
expect "bob cannot rename alice's thread" 403 "$(request -X PATCH "${bob_auth[@]}" -H 'Content-Type: application/json' -d '{"name":"Hijacked"}' "$API/threads/$thread_id")"
expect "bob cannot delete alice's thread" 403 "$(request -X DELETE "${bob_auth[@]}" "$API/threads/$thread_id")"
expect "thread details" 200 "$(request "${bob_auth[@]}" "$API/threads/$thread_id")"
expect "no email in thread responses" "" "$(grep -o '"email"' "$BODY" || true)"

if [[ "$FAILURES" -gt 0 ]]; then
  echo "$FAILURES check(s) failed"
  exit 1
fi
echo "All checks passed"

#!/usr/bin/env bash
# Creates the RSA key pair auth-service signs access tokens with. The files stay local (secrets/ is git-ignored).
set -euo pipefail

dir="$(cd "$(dirname "$0")/.." && pwd)/secrets"
mkdir -p "$dir"
if [[ -f "$dir/jwt-private.pem" ]]; then
  echo "secrets/jwt-private.pem already exists; delete it to create a new key pair"
  exit 0
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$dir/jwt-private.pem" 2>/dev/null
openssl pkey -in "$dir/jwt-private.pem" -pubout -out "$dir/jwt-public.pem"
# Readable by the non-root user inside the container; fine for local development, use a secret store elsewhere.
chmod 644 "$dir/jwt-private.pem" "$dir/jwt-public.pem"
echo "Created secrets/jwt-private.pem and secrets/jwt-public.pem"

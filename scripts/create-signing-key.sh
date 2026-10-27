#!/bin/sh
# Creates the shared signing key for CI builds and prints the four GitHub secrets to add at
#   https://github.com/troylairson/tvplayer/settings/secrets/actions
# Run once on your computer (needs `keytool`, which comes with Java / Android Studio).
# Keep the .keystore file somewhere safe: losing it means uninstalling the apps to update them again.
set -eu

KEYSTORE="${1:-tvplayer-signing.keystore}"
ALIAS="tvplayer"

if [ -e "$KEYSTORE" ]; then
  echo "$KEYSTORE already exists; pass a different path or remove it first." >&2
  exit 1
fi

# Random password; letters and digits only so it pastes cleanly
PASSWORD="$(LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 24)"

keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -storetype PKCS12 \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASSWORD" -keypass "$PASSWORD" \
  -dname "CN=tvplayer sideload" >/dev/null 2>&1

if base64 --help 2>&1 | grep -q -- '-w'; then
  B64="$(base64 -w 0 "$KEYSTORE")"   # GNU
else
  B64="$(base64 -i "$KEYSTORE")"     # macOS
fi

cat <<OUT
Created $KEYSTORE

Add these four repository secrets (Settings -> Secrets and variables -> Actions -> New repository secret):

  SIGNING_KEYSTORE_BASE64
$B64

  SIGNING_STORE_PASSWORD
$PASSWORD

  SIGNING_KEY_ALIAS
$ALIAS

  SIGNING_KEY_PASSWORD
$PASSWORD
OUT

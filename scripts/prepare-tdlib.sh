#!/usr/bin/env bash
set -euo pipefail

# Build TDLib using the official upstream Android scripts, then stage the generated
# Java bindings and native libraries so Gradle can compile/package the application.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK_DIR="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/tdlib-src"
ANDROID_DIR="$WORK_DIR/example/android"

rm -rf "$WORK_DIR"
git clone --depth 1 https://github.com/tdlib/td.git "$WORK_DIR"
cd "$ANDROID_DIR"
chmod +x ./*.sh
./check-environment.sh
./fetch-sdk.sh
./build-openssl.sh
./build-tdlib.sh

if [[ ! -d "$ANDROID_DIR/tdlib/java/org/drinkless/tdlib" ]]; then
  echo "TDLib Java bindings were not generated at the expected path." >&2
  find "$ANDROID_DIR/tdlib" -maxdepth 4 -type d -print || true
  exit 1
fi
if [[ ! -d "$ANDROID_DIR/tdlib/libs" ]]; then
  echo "TDLib native libraries were not generated." >&2
  exit 1
fi

mkdir -p "$ROOT_DIR/app/src/main/java" "$ROOT_DIR/app/src/main/jniLibs"
cp -R "$ANDROID_DIR/tdlib/java/org" "$ROOT_DIR/app/src/main/java/"
cp -R "$ANDROID_DIR/tdlib/libs/." "$ROOT_DIR/app/src/main/jniLibs/"

echo "TDLib Java bindings and native libraries staged successfully."

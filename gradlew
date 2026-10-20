#!/usr/bin/env sh
# Bootstrap wrapper: downloads Gradle distribution then executes it.
# Generated wrapper JAR is not included in this starter archive.
set -eu
GRADLE_VERSION="8.9"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/manual-gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME/gradle-${GRADLE_VERSION}/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_HOME"
  ZIP="$GRADLE_HOME/gradle-${GRADLE_VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  else
    echo "Install curl or wget to download Gradle." >&2
    exit 1
  fi
  unzip -q -o "$ZIP" -d "$GRADLE_HOME"
fi
exec "$GRADLE_BIN" "$@"

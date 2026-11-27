#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v java >/dev/null; then pkg update -y && pkg install -y openjdk-17; fi
if ! command -v gradle >/dev/null; then pkg install -y gradle; fi
if [ ! -x ./gradlew ]; then gradle wrapper --gradle-version 8.9; fi
chmod +x ./gradlew
./gradlew --no-daemon assembleDebug
printf '\nAPK created at: %s\n' "$PWD/app/build/outputs/apk/debug/app-debug.apk"

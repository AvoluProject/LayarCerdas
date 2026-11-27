#!/data/data/com.termux/files/usr/bin/bash
set -e
pkg update -y
pkg upgrade -y
pkg install -y git openjdk-17 gradle unzip wget android-sdk android-sdk-build-tools
export ANDROID_HOME="${ANDROID_HOME:-$PREFIX/share/android-sdk}"
if command -v sdkmanager >/dev/null; then
  sdkmanager --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' || echo 'SDK install skipped/failed; check sdkmanager repositories and retry.'
fi
printf '\nSetup finished. Run ./build-termux.sh from the project directory.\n'

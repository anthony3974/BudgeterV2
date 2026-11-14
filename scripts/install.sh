#!/usr/bin/env bash
# Builds Budgeter V2, installs it over the top of the current install on the phone plugged in
# over adb, and starts it.
#
#   scripts/install.sh            # debug build
#   scripts/install.sh --release  # release build (R8-minified)
#
# Both build types are signed with the same stable key (keystore.properties), so either one
# installs over the other without losing data.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGE="com.tony3974.budgeterv2"

# Gradle needs a full JDK (javac). Use JAVA_HOME if it has one, else a JDK Gradle downloaded.
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/javac" ]]; then
    for jdk in "$HOME"/.gradle/jdks/*/; do
        if [[ -x "${jdk}bin/javac" ]]; then export JAVA_HOME="${jdk%/}"; break; fi
    done
fi
ADB="${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb"

TASK=installDebug
[[ "${1:-}" == "--release" ]] && TASK=installRelease

"$ROOT/gradlew" -p "$ROOT" "$TASK" --console=plain -q
"$ADB" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
echo "Installed and started on $("$ADB" shell getprop ro.product.model | tr -d '\r')."

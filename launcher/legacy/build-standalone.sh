#!/usr/bin/env bash
# Offline build using an installed SDK; no Gradle/Maven dependency downloads required.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_JAR:?Set ANDROID_JAR to Android API 19 android.jar}"
: "${BUILD_TOOLS:?Set BUILD_TOOLS to an Android SDK build-tools directory}"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
AAPT_BIN="${AAPT2:-$BUILD_TOOLS/aapt2}"
BUILD="$PWD/build/standalone"
mkdir -p "$BUILD/classes" "$BUILD/dex"
find src/main/java -name '*.java' > "$BUILD/sources.txt"
"${JAVA_BIN}javac" -encoding UTF-8 -source 7 -target 7 -bootclasspath "$ANDROID_JAR" -d "$BUILD/classes" @"$BUILD/sources.txt"
"${JAVA_BIN}jar" cf "$BUILD/classes.jar" -C "$BUILD/classes" .
"${JAVA_BIN}java" -cp "$BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 --min-api 19 --lib "$ANDROID_JAR" --output "$BUILD/dex" "$BUILD/classes.jar"
sed 's/<manifest /<manifest package="com.gathin.tvhome.legacy" /' src/main/AndroidManifest.xml > "$BUILD/AndroidManifest.xml"
"$AAPT_BIN" compile --dir src/main/res -o "$BUILD/resources.zip"
"$AAPT_BIN" link "$BUILD/resources.zip" -I "$ANDROID_JAR" --manifest "$BUILD/AndroidManifest.xml" -A src/main/assets --min-sdk-version 19 --target-sdk-version 19 -o "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q -u "$BUILD/unsigned.apk" classes.dex)
KEYSTORE="${LEGACY_KEYSTORE:-$BUILD/legacy-debug.jks}"
if [ ! -f "$KEYSTORE" ]; then
    (umask 077; "${JAVA_BIN}keytool" -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android -alias legacy -dname 'CN=CleanYourTV Legacy Development' -keyalg RSA -keysize 2048 -validity 10000)
fi
"${JAVA_BIN}java" -jar "$BUILD_TOOLS/lib/apksigner.jar" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android --min-sdk-version 19 --v1-signing-enabled true --v2-signing-enabled false --out "$BUILD/cleanyourtv-kitkat.apk" "$BUILD/unsigned.apk"
"${JAVA_BIN}java" -jar "$BUILD_TOOLS/lib/apksigner.jar" verify --verbose --min-sdk-version 19 "$BUILD/cleanyourtv-kitkat.apk"
echo "APK: $BUILD/cleanyourtv-kitkat.apk"

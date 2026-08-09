#!/usr/bin/env bash
#
# build_offline.sh — builds the MoneyMate APK with NO network access and NO
# Gradle, using a self-contained toolchain:
#
#   JDK 8 (Corretto, committed in github.com/Fincore/install-aws-jdk8)
#   kotlinc (npm: kotlin-compiler)
#   aapt2   (npm: aaptjs3)
#   dx      (compiled from AOSP platform/dalvik with javac)
#   android.jar API 34 (github.com/Sable/android-platforms)
#
# Required env:
#   JDK8_HOME   path to a JDK 8 (bin/java, bin/javac)
#   ANDROID_JAR path to android-34/android.jar
#   AAPT2       path to the aapt2 binary
#   KOTLINC     path to the kotlinc script (kotlin-compiler npm package)
#   DX_JAR      path to dx.jar (compiled from AOSP source)
#   JRE25_HOME  path to a modern JRE (used to run kotlinc) - optional; kotlinc
#               also runs on JDK8_HOME if that is a full JDK 17+
#
set -euo pipefail

: "${JDK8_HOME:?set JDK8_HOME (JDK 8 with javac)}"
: "${ANDROID_JAR:?set ANDROID_JAR}"
: "${AAPT2:?set AAPT2}"
: "${KOTLINC:?set KOTLINC (kotlinc script)}"
: "${DX_JAR:?set DX_JAR (compiled dx.jar)}"

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
BUILD="$ROOT/.build"
rm -rf "$BUILD" && mkdir -p "$BUILD"/{gen,gen-out,classes,stdlib-clean,apk}

echo "== aapt2 compile =="
"$AAPT2" compile --dir "$ROOT/src/main/res" -o "$BUILD/res.zip"

echo "== aapt2 link =="
"$AAPT2" link -o "$BUILD/apk/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT/src/main/AndroidManifest.xml" \
  --java "$BUILD/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  "$BUILD/res.zip"

echo "== compile R.java =="
"$JDK8_HOME/bin/javac" -cp "$ANDROID_JAR" -d "$BUILD/gen-out" "$BUILD/gen/com/myexpense/tracker/R.java"

echo "== compile Kotlin sources =="
STDLIB="$ROOT/../appclassic"  # placeholder
KOTLIN_STDLIB="$(dirname "$KOTLINC")/../lib/kotlin-stdlib.jar"
find "$ROOT/src/main/java" -name "*.kt" > "$BUILD/sources.txt"
"$KOTLINC" -jvm-target 1.8 -cp "$ANDROID_JAR:$BUILD/gen-out:$KOTLIN_STDLIB" \
  -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "== prepare stdlib for dexing =="
(cd "$BUILD/stdlib-clean" && "$JDK8_HOME/bin/jar" xf "$KOTLIN_STDLIB" && rm -rf META-INF && "$JDK8_HOME/bin/jar" cf ../stdlib.jar .)

echo "== dex =="
"$JDK8_HOME/bin/java" -Xmx2g -cp "$DX_JAR" com.android.dx.command.Main \
  --dex --min-sdk-version=26 --output="$BUILD/apk/classes.dex" \
  "$BUILD/classes" "$BUILD/gen-out" "$BUILD/stdlib.jar"

echo "== package =="
(cd "$BUILD/apk" && "$JDK8_HOME/bin/jar" uf base.apk classes.dex)

echo "== sign =="
if [ ! -f "$BUILD/debug.keystore" ]; then
  "$JDK8_HOME/bin/keytool" -genkeypair -keystore "$BUILD/debug.keystore" \
    -alias androiddebugkey -storepass android -keypass android \
    -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000
fi
"$JDK8_HOME/bin/jarsigner" -keystore "$BUILD/debug.keystore" -storepass android -keypass android \
  -sigalg SHA1withRSA -digestalg SHA1 "$BUILD/apk/base.apk" androiddebugkey

mkdir -p "$OUT"
cp "$BUILD/apk/base.apk" "$OUT/MoneyMate-v1.0.0.apk"
echo ""
echo "✅ APK ready: $OUT/MoneyMate-v1.0.0.apk"

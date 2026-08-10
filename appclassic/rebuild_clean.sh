#!/usr/bin/env bash
set -euo pipefail
cd /home/user/spendify/appclassic
JDK8=/tmp/final/jdk8/usr/lib/jvm/java-1.8.0-amazon-corretto
ANDROID_JAR=/tmp/final/platforms/android-34/android.jar
AAPT2=/tmp/final/aaptjs3/bin/x64/linux/aapt2
KOTLINC=/tmp/tc2/kotlinc/bin/kotlinc
DX_JAR=/tmp/final/dx.jar
export PATH=/tmp/tc2/jdk4py/jdk4py/java-runtime/bin:$PATH

rm -rf .build/clean && mkdir -p .build/clean
$AAPT2 compile --dir src/main/res -o .build/clean/res.zip
$AAPT2 link -o .build/clean/base.apk -I $ANDROID_JAR --manifest src/main/AndroidManifest.xml --java .build/clean/gen --min-sdk-version 26 --target-sdk-version 34 .build/clean/res.zip
mkdir -p .build/clean/gen-out
$JDK8/bin/javac -cp $ANDROID_JAR -d .build/clean/gen-out .build/clean/gen/com/myexpense/tracker/R.java
find src/main/java -name "*.kt" > .build/clean/sources.txt
$KOTLINC -jvm-target 1.8 -cp "$ANDROID_JAR:.build/clean/gen-out:$(dirname $KOTLINC)/../lib/kotlin-stdlib.jar" -d .build/clean/classes @.build/clean/sources.txt
mkdir -p .build/clean/stdlib-clean
(cd .build/clean/stdlib-clean && $JDK8/bin/jar xf "$(dirname $KOTLINC)/../lib/kotlin-stdlib.jar" && rm -rf META-INF && $JDK8/bin/jar cf ../stdlib.jar .)
$JDK8/bin/java -Xmx2g -cp $DX_JAR com.android.dx.command.Main --dex --min-sdk-version=26 --output=.build/clean/classes.dex .build/clean/classes .build/clean/gen-out .build/clean/stdlib.jar
(cd .build/clean && $JDK8/bin/jar uf base.apk classes.dex)

mkdir -p .build/clean/sign && cp .build/clean/base.apk .build/clean/sign/
echo "=== uber sign UNSIGNED base ==="
$JDK8/bin/java -jar /tmp/final/package/vendor/uber-apk-signer.jar --apks .build/clean/sign --allowResign --overwrite --ks keystore/moneymate.keystore --ksAlias moneymate --ksPass moneymate123 --ksKeyPass moneymate123 2>&1 | tail -3
echo "=== verify ==="
$JDK8/bin/java -jar /tmp/final/apksigner/apksigner.jar verify --verbose .build/clean/sign/base.apk 2>&1 | grep -E "Verifies|v1 scheme|v2 scheme|v3 scheme"

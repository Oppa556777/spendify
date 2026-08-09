# MoneyMate – classic (framework-only) build

This module is a **second, dependency-free implementation** of MoneyMate that
uses **only the Android framework** (`android.jar`) + the Kotlin standard
library — no AndroidX, no Compose, no Room, no Hilt, no Gradle.

It exists because it can be compiled in a **fully offline, network-restricted
environment** with a hand-assembled toolchain, producing a verified,
installable APK:

- JDK 8 (Amazon Corretto RPM, committed in `github.com/Fincore/install-aws-jdk8`)
- `kotlinc` 2.4.10 (npm package `kotlin-compiler`)
- `aapt2` 2.20 (npm package `aaptjs3`)
- `dx` 1.16 (compiled from AOSP `platform/dalvik` with `javac`)
- `android.jar` API 34 (`github.com/Sable/android-platforms`)

Same feature set as the main `app` module: dashboard, transactions with
filters/search, categories & accounts management, monthly budgets,
statistics (donut + 12-month chart), CSV/PDF export, JSON backup/restore,
biometric/device-credential lock, light/dark/system themes, Nunito + Inter
fonts bundled.

## Build (offline)

```bash
export JDK8_HOME=/path/to/jdk8
export ANDROID_JAR=/path/to/android-34/android.jar
export AAPT2=/path/to/aapt2
export KOTLINC=/path/to/kotlinc
export DX_JAR=/path/to/dx.jar
./build_offline.sh
# → out/MoneyMate-v1.0.0.apk
```

The script performs the classic manual pipeline:

```
aapt2 compile --dir res            → compiled resources
aapt2 link   -I android.jar        → base.apk + R.java
javac  R.java
kotlinc      -cp android.jar       → classes
dx           --dex                 → classes.dex
jar uf base.apk classes.dex
jarsigner                          → signed APK
```

## Notes

- The manifest declares **no INTERNET permission**; the app is fully offline.
- minSdk 26 / targetSdk 34; signed with a debug key (v1 scheme) for sideloading.
- Dark mode works through `values-night/` resources; the Settings screen can
  force Light/Dark/System.

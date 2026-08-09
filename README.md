# MoneyMate – Expense Tracker

A complete, **100% offline** personal expense tracker for Android, inspired by
*Paisa – Spending Tracker*. Every feature — including the "premium" ones — is
free, unlocked by default, and works forever without an internet connection.

- ❌ No internet permission (there is **no** `INTERNET` permission in either manifest)
- ❌ No login, no sign-up, no ads, no subscriptions, no cloud
- ✅ All data stored locally in SQLite on the device
- ✅ Works fully offline forever; supports Android 8.0 (API 26) and up
- ✅ Ships as a signed, installable APK (`apk/MoneyMate-v1.0.0.apk`)

## Two implementations

| | `app/` (primary) | `appclassic/` (offline-built) |
|---|---|---|
| UI | **Jetpack Compose + Material 3** (declarative) | Android Views (framework-only) |
| Database | **Room** (SQLite) + Flow | SQLiteOpenHelper (SQLite) |
| DI | **Hilt** | manual |
| Navigation | **Jetpack Navigation Compose** | activities |
| Charts | **Vico** + Canvas donut | Canvas donut/bar views |
| Build | Gradle (Kotlin DSL) + wrapper | `build_offline.sh` (no Gradle) |
| APK | built with `./gradlew :app:assembleDebug` | **the APK in `apk/` was built from this module** |

Both share the same package `com.myexpense.tracker`, the same feature set and
the same offline-first rules. The `appclassic` module exists because it can be
built in a fully network-restricted environment with a hand-assembled
toolchain — see `appclassic/README.md` for the full offline build recipe.

## Features

- **Dashboard** – total balance hero card, monthly income/expenses, recent
  transactions, budget progress
- **Transactions** – add / edit / delete expenses & incomes with category,
  account, date and note; filter by month, type, category, account; search
- **Categories** – 18 built-in defaults, fully editable; pick from emoji icons
  (classic) / 100+ Material icons (Compose) and a color palette
- **Accounts** – cash, bank, card, e-wallet… with live balances
- **Budgets** – monthly per-category limits with progress bars & overspend
  warnings
- **Statistics** – category donut + breakdown, 12-month income/expense chart
- **Settings** – theme (system/light/dark), currency symbol, biometric lock
- **Export** – CSV (all transactions) and PDF monthly report
- **Backup/Restore** – full JSON backup to any local folder, with preview
- **Security** – device-credential / biometric lock (framework
  `KeyguardManager`, no Play-services dependency)

## Building the Compose app (normal environment)

```bash
./gradlew :app:assembleDebug        # needs JDK 17+, Android SDK 34
```

## Building in a restricted-egress environment

`ci/toolchain-mirror.yml` is a GitHub Actions workflow that builds the Compose
app on a GitHub-hosted runner (clean egress) and mirrors the full offline
toolchain (JDK + Gradle + Android SDK + pre-warmed Gradle cache) into the
`toolchain-mirror` branch as split archives with `SHA256SUMS`. Copy it to
`.github/workflows/` and push with an account that has the `workflows`
permission (this repo was authored by a GitHub App without it, which is why
the file lives under `ci/`).

For a no-Gradle build, use `appclassic/build_offline.sh` (see
`appclassic/README.md`) — this is how `apk/MoneyMate-v1.0.0.apk` was produced
and verified (`aapt2 dump badging` + `jarsigner -verify`).

## Project layout

```
app/                         Jetpack Compose + Room + Hilt implementation
appclassic/                  framework-only implementation + offline build
apk/                         the built, signed APK (MoneyMate-v1.0.0.apk)
ci/                          CI workflow (copy to .github/workflows/ to use)
```

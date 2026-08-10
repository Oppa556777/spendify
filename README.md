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

- **Splash + Onboarding** – animated gradient splash (Android 12+ SplashScreen
  API with compat), 4-step onboarding with a HorizontalPager and animated
  vector illustrations, then a one-time setup sheet (currency, primary
  account, starting balance, fingerprint lock) — never shown again
- **Dashboard** – animated hero balance card (gradient, eye toggle with blur,
  per-account swipe switcher), period filter chips (Today…Custom), 7-day
  spending bar chart with tap tooltips, quick actions, animated budget
  progress cards, swipe-to-edit/delete transactions with a detail bottom
  sheet, savings goal rings, subscription reminders, floating pill bottom nav
  with a spring-animated + speed dial
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

## Database schema (Room, 15 tables)

| Table | Purpose |
|---|---|
| `accounts` | bank/cash/card/wallet accounts with stored `balance` (Double), `currency`, `colorHex`, `iconName`, `isDefault` |
| `categories` | income/expense categories with `iconName`, `colorHex`, nested via self-referencing `parentId` |
| `transactions` | central ledger: `title`, `amount` (Double), `type` (INCOME/EXPENSE/TRANSFER), `categoryId`/`accountId` (NOT NULL FKs), `toAccountId`, `note`, `date` (epoch millis), `time` (HH:mm), `tags`, `personId`, location fields, `receiptImagePath`, `isRecurring`/`recurringId` |
| `budgets` | per-category (or all-category) limits with `limitAmount`, `spentAmount`, `period` (WEEKLY/MONTHLY/YEARLY/CUSTOM), `alertAt` %, `isActive` |
| `goals` | savings goals: `targetAmount`, `savedAmount`, `deadline`, progress computed in SQL |
| `loans` | LENT/BORROWED loans with `amount`, `paidAmount`, `dueDate`, `isSettled` |
| `subscriptions` | recurring subscriptions with `billingCycle`, `nextDueDate`, `reminderDays` |
| `tags` | named tags with colours |
| `people` | contacts for loans/splits (`name`, `phone`, `avatarColor`) |
| `recurring_rules` | templates that generate recurring transactions (`frequency`, `interval`) |
| `bill_splits` / `bill_split_members` | shared bills + per-person shares (`shareAmount`, `isPaid`) |
| `assets` | portfolio: STOCK/MUTUAL_FUND/CRYPTO/REAL_ESTATE/GOLD/FD/OTHER with `quantity`, `buyPrice`, `currentPrice` |
| `achievements` | gamification (seeded with 8 samples on first launch) |
| `app_settings` | key-value store |

Amounts are stored as `Double` rupees in the DB; the domain layer maps them to
`Long` minor units to avoid floating-point drift. Dates are epoch-millisecond
timestamps (UTC midnight); the repository layer converts to/from `LocalDate`.
Enums are stored as their names via Room's built-in converters.

DAOs expose full CRUD plus complex queries: monthly income/expense totals
(`strftime('%Y-%m', date/1000, 'unixepoch')`), per-category totals, date-range
filters, title/note/category search, period income-vs-expense, budget
remaining & progress (computed in SQL), goal progress percentages, loan
outstanding balances, subscription due windows, asset portfolio value, and
bill-split paid/total shares. Deleting a category/account safely reassigns
referencing transactions/subscriptions/recurring rules to a fallback first.

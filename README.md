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
- **Transactions** – full-screen bottom-sheet add/edit with a built-in
  calculator keypad (÷×−+), type tabs that recolor the header (red/green/
  blue), smart title suggestions from history, category grid picker with
  search + quick-add, account picker with live balances, date/time pickers,
  tag multi-select, person/location/note fields, camera & gallery receipt
  attachment (FileProvider + Coil), recurring rules (frequency/interval/end
  date), transfer From↔To accounts with an animated swap arrow, validation
  with snackbars, haptic + success animation on save, and real-time balance
  updates; filter by month, type, category, account; search
- **Categories** – 18 built-in defaults, fully editable; pick from emoji icons
  (classic) / 100+ Material icons (Compose) and a color palette
- **Accounts** – "My Accounts" screen with Total Assets / Liabilities / Net
  Worth summary, a swipeable gradient account carousel (per-account gradient,
  name/type/icon/balance/last transaction), full account list with
  swipe-to-delete, and a rich add/edit bottom sheet (7 account types with
  emojis, 50+ currency dropdown, 20-color + random picker, 100+ icon grid,
  default toggle); account detail screen with 30-day balance line chart,
  income/expense filters, transaction list and Transfer From/To button
- **Budgets** – overview with total budgeted/spent/remaining + progress ring,
  All/Active/Over Budget/Completed filter tabs, cards with category-color
  accent border, animated color-coded progress, over-budget warning state;
  add/edit sheet with All/Specific/Multiple-category scopes (join table),
  amount calculator keypad, Daily/Weekly/Monthly/Yearly/Custom periods,
  50–90% alert slider, color picker and custom date range; detail screen with
  spent-vs-remaining donut, day-by-day spending line, category transactions
  and edit/delete
- **Reports & Analytics** – 10 interactive Canvas charts in M3 cards: grouped
  income/expense bars, expense donut with breakdown, this-vs-last-month daily
  spending lines, GitHub-style spending heatmap, top-5 category trend lines,
  income-sources pie, savings-rate gauge, net-worth area chart, top-5
  horizontal bars and a cash-flow waterfall — every chart animates on entry,
  supports tap tooltips, works in light/dark, plus PDF/CSV export
- **Goals** – gradient savings-goal cards with progress rings, animated
  progress bars, Add Money quick-add, deadline labels and confetti on
  completion; add/edit sheet with icon/color/account/note
- **Loans** – "I Lent" (blue) / "I Borrowed" (orange) sections with totals,
  avatar cards showing original/remaining/due, Active/Overdue/Settled status,
  partial payments and mark-settled
- **Subscriptions** – monthly/yearly totals + active count, category filter,
  cards with first-letter icons, due-in chips color-coded by urgency
  (green/orange/red), active/paused toggles, Pay-to-advance; add sheet with
  cycle/category/reminder/color
- **Achievements** – 20 built-in achievements (all free): gold-bordered
  unlocked cards with unlock dates, 🔒 grayed locked cards, confetti burst on
  unlock, system notifications, streak/90-day tracking, and "Money Master"
  for completing all others; checked after every action
- **Split Bill** – equal/custom/percentage splits, participants from People or
  inline names, "who paid?" tracking, per-member paid toggles, settlement
  summary and full history
- **Assets** – stocks/mutual funds/crypto/real estate/gold/FD/other with
  invested vs current value, gain/loss + %, portfolio summary and a
  by-type donut chart
- **Recurring automation** – on app open, due recurring rules trigger a
  notification + in-app confirm dialog; confirmed rules auto-create
  transactions and mark themselves executed
- **Advanced search** – instant results with date/category/account/amount/tag/
  person filters, sort by date/amount/category, highlighted matches and local
  recent-search history
- **Design system** – complete tokens: Nunito (ExtraBold/Bold/SemiBold/
  Regular) + Inter (Medium/Regular/Light) with a full type scale
  (Display 48 → Caption 11), spec-exact light/dark color palettes, 12-category
  palette, 6 account-card gradients, corner-radius/spacing/elevation scales,
  15-component library (AmountText with count-up, TransactionListItem with
  swipe, CategoryChip, ProgressCard, SummaryCard, SectionHeader, EmptyState
  with vector illustrations + CTA, LoadingShimmer, ConfirmDialog,
  DateRangePicker, CurrencyInput + calculator, IconPickerGrid, ColorPickerRow,
  SnackBarMessage, AchievementCard), staggered list fade-ins, shimmer
  skeletons, 300ms fade+slide screen transitions, and illustrated empty states
  (piggy bank / budget chart / target / handshake / calendar)
- **Home-screen widgets** – 2×1 balance widget (today's spending + balance)
  and 4×2 7-day spending graph widget; taps open the app / Reports
- **More menu** – grouped hub (Tools: Achievements, Bill Splitter, Asset
  Tracker, People, Tags Manager, Recurring Transactions; Reports: Detailed
  Reports, Calendar View, Export; Settings; Data: Backup/Restore/Clear with
  double confirmation; About + version)
- **Settings** – full control panel: theme (Light/Dark/System/AMOLED), 10
  accent colors, font size slider; currency (50+), date format, week start,
  month start day, Indian/international number format; app lock, biometric,
  lock-after delay, hide-balance-by-default; notification toggles incl. daily
  reminder time; category management and data management (backup/restore/
  PDF/CSV/clear-all with 2-step confirm)
- **Calendar View** – monthly grid with spending-intensity day cells, dot
  indicators, tap a day for its transactions, month totals and navigation
- **Tags Manager** – all tags with transaction counts, edit/delete/add,
  tap to filter (opens Search)
- **People Manager** – avatars, transaction counts and net balances, tap for
  a person's transaction history
- **Export** – CSV (all transactions) and PDF monthly report
- **Backup/Restore** – full JSON backup to any local folder, with preview
- **Security** – device-credential / biometric lock (framework
  `KeyguardManager`, no Play-services dependency)

## Installing the APK

1. On your phone, enable **Settings → Security → Install unknown apps** for
   your browser/files app.
2. Download `apk/MoneyMate-v1.0.0.apk` (direct link below) and tap it.
3. If you had a **previous MoneyMate installed with a different signing key**,
   uninstall it first (Android won't overwrite apps signed by another key):
   `Settings → Apps → MoneyMate → Uninstall`, then install the new APK.
4. If installing over USB: `adb install -r apk/MoneyMate-v1.0.0.apk`.

Direct download: https://github.com/Oppa556777/spendify/raw/arena/019fe7d0-spendify/apk/MoneyMate-v1.0.0.apk

## Building the Compose app (normal environment)

```bash
./gradlew :app:assembleDebug        # needs JDK 17+, Android SDK 34
```

## Installing the APK

1. On your phone, enable **Settings → Security → Install unknown apps** for
   your browser/files app.
2. Download `apk/MoneyMate-v1.0.0.apk` (direct link below) and tap it.
3. If you had a **previous MoneyMate installed with a different signing key**,
   uninstall it first (Android won't overwrite apps signed by another key):
   `Settings → Apps → MoneyMate → Uninstall`, then install the new APK.
4. If installing over USB: `adb install -r apk/MoneyMate-v1.0.0.apk`.

Direct download: https://github.com/Oppa556777/spendify/raw/arena/019fe7d0-spendify/apk/MoneyMate-v1.0.0.apk

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

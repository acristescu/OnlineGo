# Kotlin Multiplatform migration

Goal: OnlineGo on Kotlin Multiplatform with **iOS as the second target** and **Compose
Multiplatform** for all UI, without a long-lived branch - **the Android app builds and ships at the
end of every phase.**

**Status.** Phases 0-4 are done and shipped (`origin/release`, tag `beta_b1064`): the data layer,
all screens, navigation and the app shell live in `:shared/commonMain`, and `:app` is only the
Android shell. Phase 5 (the iOS app) has started: `iosApp/` runs the shared UI in the Simulator
with in-process logout, remote images, puzzle HTML and in-app language switching. The remaining
platform bindings are stubbed. `docs/IOS_DEVELOPMENT.md` covers building and running it.

Slice numbers (6.x) in commit messages map to the full write-ups in earlier versions of this file:
`git show 9cf6cb29:docs/KMP_MIGRATION_ASSESSMENT.md` (6.1-6.56) and
`git show d54399dc:docs/KMP_MIGRATION_ASSESSMENT.md` (up to 6.61).

---

## 1. What is left

### 1.1 iOS app (Phase 5) - 4-8 weeks, excluding local AI

- **Notifications and background refresh**: move the checks in `CheckNotificationsTask` /
  `SynchronizeGamesWork` to common code, then `BGAppRefreshTask` + `UNUserNotificationCenter`.
  Turn notifications will be slower than Android's 15-minute poll without APNs, which OGS doesn't
  offer.
- **Stubbed bindings**: `Analytics` and `GameNotificationCanceller` (no-ops from Swift),
  `CrashReporter` (logs only), `ReviewRequester` (needs `SKStoreReviewController`),
  `SupporterStore` (StoreKit or RevenueCat, plus iOS wording for the supporter copy), Google
  sign-in (always reports "cancelled").
- **App Store plumbing**, and a macOS stack on Bitrise.

### 1.2 Open decisions

- **Does iOS v1 ship without local AI?** KataGo runs as a child process, which iOS forbids; it
  would need relinking as an in-process library, and its memory then shares the app's jetsam
  budget. Recommendation: ship without it and hide the entry point (iOS binds
  `UnavailableAnalysisEngine` today, which shows an error state).
- **`minSdk` 26** would drop core library desugaring. It is 24 today.

### 1.3 Kept long-term

The legacy cookie import (`CookiePersistence` prefs, `SerializableCookie`): delete once Play
Console shows pre-6.5 installs are negligible, or those users get logged out.

### 1.4 On-device checks still owed

- The seven untested body-bearing endpoints: `createAccount`, `openChallenge`, `challengePlayer`,
  `markPuzzleSolved`, `ratePuzzle`, `acknowledgeWarning`, `deleteAccount`.
- Crashlytics breadcrumbs arriving as `I/Tag: message`; release logcat silent.
- The websocket on logout: `cleanup()` sent before the close frame.
- Game connection ref-counting: reopen a game; an active game keeps receiving moves after its
  screen closes; chat turns on when the screen opens.
- The review prompt after a win (blocked by Play's quota on the developer account).
- Notifications suppressed while the app is in the foreground.

---

## 2. Architecture

```
:shared    KMP library - model, logic, network, database, data, UI
:app       Android shell - MainActivity, notifications, WorkManager, Play Billing, Play Review
iosApp/    Xcode project - UIKit shell, later BGTaskScheduler, StoreKit, notifications
```

Koin follows the same layers: `sharedModule` (commonMain), `androidPlatformModule` /
`iosPlatformModule` (the platform contract), and each app as the composition root. On iOS that is
`initKoin()` in `iosMain`, called from Swift.

Toolchain: Kotlin 2.4.21, AGP 9.3.3, Gradle 9.7.1, Compose Multiplatform 1.13.0-alpha02 (alpha for
fixes only there - move to 1.13 stable when it ships), Material3 pinned to 1.9.0.

Tests: 237 - 148 in `commonTest` (run on Android host and iOS simulator), 20 Android-only host
tests and 65 `iosTest` in `:shared`, 4 in `:app`.

---

## 3. Traps

Things that break silently if changed:

- **Serialization**: every DTO file needs `@file:UseSerializers` with the lenient Int/Long
  serializers (OGS sends `99999999.0` for ints).
- **Cookies**: only `csrftoken` and `sessionid`, only for the OGS host. A third required cookie
  would break login.
- **Room**: schema 20 with `fallbackToDestructiveMigration` - a bad migration silently wipes
  caches.
- **Score estimator JNI**: renaming or moving `NativeEstimator` breaks scoring silently
  (`UnsatisfiedLinkError` is only logged).
- **Strings**: Crowdin exports Android escapes (`\'`, `\"`, `%%`) that Compose resources show
  literally. A Custom Post-Export Processor unescapes them; if it stops running, translations
  break.
- **Drawables**: Compose's vector parser only accepts literal `#` colors; `@color/` references
  throw at load time. `drawable-dark/` follows the system theme, not the app's theme setting.
- **Routes**: each `@SerialName` is also the analytics screen name and the deep-link shape.
- **Coil**: each app must register a network fetcher, or remote images silently show the
  placeholder.
- **Material icons**: compiled against Compose 1.7; recheck after every Compose upgrade.
- **iOS language**: `NSLocale.currentLocale` is fixed at launch, so iOS formatting must use
  `appLocale()`. A language change redraws the UI from the start screen.
- **iOS `ViewModelStore`**: Compose on iOS never clears it; `AppSession` does, and logout depends
  on it.
- **iOS tests**: the standalone simulator test run fails every TLS handshake; real-network tests
  need a booted simulator.
- **Known bug, deliberately unfixed**: the Joseki label regex skips columns A-H with two-digit
  rows (`[A-H]|[J-T]\d{1,2}` should be `[A-HJ-T]\d{1,2}`).

# Kotlin Multiplatform migration

Goal: move OnlineGo to Kotlin Multiplatform with **iOS as the second target** and **Compose
Multiplatform as the end state**, without a long-lived branch - **the Android app builds and ships
at the end of every phase.**

**Verdict: feasible.** The UI is 100% Compose (one XML layout, a notification `RemoteViews`), there
is no RxJava, no `Parcelable`, and Koin, Molecule, Room, DataStore, `ViewModel`, Turbine and
kotlinx-collections-immutable are already multiplatform. The cost is dominated by resources
(663 strings x 16 locales) and the local-AI engine, which cannot run on iOS as built today.

**Status.** Phases 0-4 are finished: fifty-five slices are done (section 6), and all of them have
shipped - `origin/release` and the `beta_b1064` tag both point at the commit with the last Phase 4
slice (6.55). Several slices still need on-device checks (section 1.2), which now applies to a
build already in users' hands rather than one still pending.
Phase 3 moved the Room
database (6.25), the score estimator (6.26), `CrashReporter` (6.10), the HTTP client (6.27),
storage (6.28), the session cluster (6.29) and the remaining repositories (6.30) to common code,
wired by a shared Koin module (6.31) and an iOS platform module (6.32). Only the bindings Swift
supplies are left, and they come with the iOS app. Phase 4 put all of the UI in common code: the
toolchain is set for
Compose Multiplatform (6.33), now on 1.13.0-alpha02 (6.35), the first composable is common code
(6.34), strings (6.36) and the UI drawables (6.39) are Compose Multiplatform resources on both
platforms, `BoardView` is gone (6.37) along with the resources and dependencies nothing used
(6.38), the board is common code with its assets (6.40), and so are the Material icons (6.41),
the ViewModel plumbing, the theme and the first screen, Learn (6.42), then the puzzle screens,
socket debug and the automatch sheet (6.43), the tutorial and face-to-face screens (6.44), and the
My Games tab with new challenges and What's new (6.45), the game screen (6.46), the AI game
(6.47), the Joseki explorer with the bottom bar (6.48), the rank chart on a Compose `Canvas` (6.49),
the Stats screen (6.50), Settings with the language setting (6.51), Supporter on a billing
contract (6.52), Onboarding (6.53), the navigation host (6.54), and the app shell's theme, session
and start-up logic (6.55). `:app` is now only the Android shell. Phase 5 has started: `iosApp/` runs
the shared UI in the
Simulator (6.57), with the platform bindings still stubbed. `docs/IOS_DEVELOPMENT.md` covers
building and running it.

Earlier, longer versions of this document:
`git show d529259:docs/KMP_MIGRATION_ASSESSMENT.md` (original assessment),
`git show acedf6a:docs/KMP_MIGRATION_ASSESSMENT.md` (full write-ups of slices 6.1-6.15), and
`git show 9cf6cb29:docs/KMP_MIGRATION_ASSESSMENT.md` (full slice-by-slice write-ups of 6.1-6.56,
before section 6 was condensed down to what's still load-bearing).

---

## 1. What is left

### 1.1 Phase 0 - de-coupling (Android-only, no KMP tooling)

Nothing left.

**Kept long-term:** the legacy `CookiePersistence` prefs file, the `SerializableCookie` shim and
the import branch (6.5). Users update late, and anyone coming straight from a pre-6.5 version is
logged out without them. Delete them only once the Play Console shows pre-6.5 installs are
negligible.

**Product call:** raising `minSdk` to 26 is the only way to drop core library desugaring (6.7). It
is
24 since 6.35.

### 1.2 On-device checks owed for finished work

Verified on device: the session survives an in-place upgrade, password login, Google sign-in,
declining a challenge (an unsafe request, so CSRF works), the score estimate after both JNI
renames (6.24, 6.26), Room in `commonMain` installed over the existing app (6.25), tutorials
opening with `tutorials.json` still in the app (6.22), storage in `commonMain` (6.28), the Koin
graph after 6.31, logout (6.29), tutorials and Joseki after 6.30, type-safe navigation on a
release (R8) build (6.19), an idle websocket staying up past a minute after 6.32, and the Tsumego
puzzle picker from `commonMain` (6.20, 6.34): scrolled to the current puzzle, lazy thumbnails,
tap to select, and closing without reopening. Also: a game notification drawn by `renderBoard`
(6.37) arriving in the chosen language (Romanian), the app after 6.38, every screen touched by
6.39 in light and dark, and the board from `commonMain` (6.40): every style in light and dark,
coordinates, drag and lift to play, both fades, the Settings previews, AI hints and ownership,
Joseki and tutorial marks, label centring, and a notification board (Settings -> Debug -> Test
Notification). After 6.42: the Learn tab, a live game and face-to-face on `Immediate`, screens
with route arguments, dynamic colours on Android 12+, and the `LearnUI` preview in Android Studio.
After 6.43: the puzzle directory (filter, sort, search, solve rates in German), Tsumego (solving,
hints, the picker, rating, a description with a link), socket debug and the automatch sheet. After
6.44: a tutorial in both orientations played to the end, face-to-face in both orientations (moves,
undo and redo, clocks, the saved game after a restart), and rotating the game, AI game, Joseki and
My Games screens. After 6.45: What's new not reappearing after the update, cold starts drawing the
boards with their background from the first frame, the Play tab, the large game card in landscape,
a custom game through the new-challenge sheet and opponent picker, back in every dialog and
screen that handles it, and the translation-guide link. After 6.46: the stone sound on moves, a
game opening without a hitch after a cold start, the screen staying on in a game, a game's
notification disappearing when it opens, a link from a game, chat, player details and the
game-over dialog with its rating. After 6.47: KataGo starting and an AI game played. KO, the resign
offer and
restore were not checked one by one: the move moved the code unchanged, and their logic is
covered by `AiGameViewModelTest`, which now runs on both platforms. After 6.48: the Joseki root
description, a position with YouTube links (the "(video)" label, the link colour in light and
dark), a link opening YouTube, and the bottom bar. After 6.51: changing the language on Android
13+ and below 13 (the recreate from `MainActivity`), the system per-app language on 13+ showing
in Settings, the notifications row, the translation-guide and privacy links, the build version,
the review-prompt diagnostic, a test notification, and logout, delete account and the avatar.
After 6.52: a failed billing query showing as a snackbar on Supporter, and, on a production
build, tiers and prices loading, the slider and subscribe button, the billing sheet, an upgrade
or downgrade from an active subscription, and the manage-subscription link - all fine. After
6.53: the normal onboarding flow (info pages, back, password login and sign-up, wrong
credentials, staying offline, the notification permission page, login and sign-up from the Play
tab), and, on a release build, Google sign-in itself - fine. After 6.54:
every tab and back from every screen, the bottom bar keeping each tab's state, the Stats login
sheet when logged out, opening a game from a notification with the app closed and in the
background, backing out of onboarding on a first start closing the app, and the review-prompt and
test-notification callbacks from `MainActivity`. Also verified: the Ktor websocket connecting,
authenticating and reconnecting over several days of play on the dev build (6.14); clock text
(6.16); switching language with strings from Compose Multiplatform resources (6.36); Joseki
position links (6.48); and Stats logged in, light and dark, in `de` (6.49, 6.50). After 6.55: the
board background on the first frame of a cold start, switching app theme and system dark mode
while running with readable system bar icons, a board theme change, the socket connecting on
resume and disconnecting in the background, the avatar on the first frame, and a language change
below 13 recreating.

Still owed - unit tests cannot reach these:

- [ ] **The other seven body-bearing endpoints** (6.4): `createAccount`, `openChallenge`,
  `challengePlayer`, `markPuzzleSolved`, `ratePuzzle`, `acknowledgeWarning`, `deleteAccount`.
- [ ] **Crashlytics breadcrumbs** arrive as `I/Tag: message`, and release logcat is silent (6.6).
- [ ] **The Ktor websocket on logout** (6.14): `cleanup()` is sent before the close frame. Not
  visible in normal use, so it needs the socket debug screen or logs.
- [ ] **Game connection ref-counting** (6.15): open, leave and reopen a game; a game in the active
  list keeps receiving moves after its screen closes; chat turns on when the screen opens.
- [ ] **The review prompt after a win** (6.46): now requested through `ReviewRequester`, which
  `MainActivity` provides. Not reproducible on the developer account, which has hit Play's quota.
- [ ] **Notifications suppressed in the foreground** (6.55): `isInForeground` is still set by
  `MainActivity`, but the ViewModel's resume/pause moved to `OnlineGoRoot`.

### 1.3 Phases 3-5

| Phase                              | Work                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Estimate                      |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| **3 - data layer to `commonMain`** | Done (6.25, 6.27-6.32). Left for the iOS app (the shell passes no-op `Analytics`, `AppDataEraser` and `GameNotificationCanceller` to `initKoin()` since 6.57): real versions of those three, a real iOS `CrashReporter`, the iOS HTML placeholder (6.43), Coil's Ktor network fetcher (6.45), an iOS `ReviewRequester` (6.46), a "restart to apply" notice after a language change (6.51), and a `SupporterStore` (StoreKit or RevenueCat) with iOS wording for the supporter copy, which names Android and Google Play (6.52), and Google sign-in, which reports "cancelled" for now (6.53). | done                          |
| **4 - Compose Multiplatform**      | UI into `:shared/commonMain`, screen by screen. Resources (4.2), chart, markdown. Done: the board (6.40), icons (6.41), ViewModel plumbing, theme, Learn (6.42), puzzles, socket debug, automatch (6.43), tutorial, face-to-face (6.44), My Games (6.45), game (6.46), AI game (6.47), Joseki (6.48), Stats (6.49, 6.50), Settings (6.51), Supporter (6.52), Onboarding (6.53), navigation (6.54).                                                                                                                                                                                            | done                          |
| **5 - iOS app**                    | Shell done (6.57). Left: notifications, background refresh, billing, Google sign-in, App Store plumbing. Bitrise needs a macOS stack.                                                                                                                                                                                                                                                                                                                                                                                                                                                         | 4-8 weeks, excluding local AI |

Estimates are order-of-magnitude, single developer. The one data point so far: the serialization
slice came in on time only because the golden-JSON corpus was dropped, which moved verification
onto the device, where its one real bug surfaced. Budget for where verification happens, not just
for writing the code.

---

## 2. Target architecture: three modules

```
:shared    KMP library - model, logic, network, database, data, and (from phase 4) UI
:app       Android application - MainActivity, notifications, WorkManager, Play Billing,
           Play Review, Google sign-in
iosApp/    Xcode project - UIKit shell, BGTaskScheduler, StoreKit, UNUserNotificationCenter
```

Layering inside `:shared` lives in package names; `commonMain` / `androidMain` / `iosMain` do the
job a module split would, since the compiler rejects `android.*` in `commonMain`.

The Koin graph follows the same three layers (6.31): `sharedModule` in `commonMain` defines
everything platform-independent, `androidPlatformModule` / `iosPlatformModule` implement the
platform contract, and each app is the composition root - it calls `startKoin` and adds app-only
bindings. On iOS the root is a Kotlin `initKoin()` in `iosMain` that Swift calls, since Swift
cannot write Koin modules.

A nine-module `:core:*` split is the default advice and wrong here: no `build-logic` (nine
hand-kept build files on a brand-new toolchain), one `Res` class per module fragments the
resources work, `model` + `logic` is 950 LOC, and nearly every commit has one author. Revisit when
clean-build time becomes a real problem (record a baseline first), a second regular contributor
appears, or local AI needs Play Feature Delivery for its 265 MB of assets. Baseline after Phase 1:
`./gradlew --rerun-tasks --no-build-cache :app:assembleDebug` takes ~26 s (M4 Pro, warm daemon).

---

## 3. Dependencies

**Done (section 6):** Moshi and `org.json`, jsoup, Coil 2, Retrofit, PersistentCookieJar,
`android.util.Log`, `java.time`, direct OkHttp use, Firebase Analytics and Crashlytics call sites,
`java.util.concurrent` / `UUID` / `Stack`, string navigation routes, `BuildConfig.DEBUG` in
`HTTPConnectionFactory` (6.23), Firebase Analytics and Crashlytics in common code (6.9, 6.10),
DataStore (6.28), `BoardView` (6.37), `lifecycle-extensions` and `firebase-messaging` (6.38),
`material-icons-extended` (6.41), `koin-androidx-compose` and Molecule's `AndroidUiDispatcher` (
6.42), Markwon (6.48), MPAndroidChart and JitPack (6.49), `AppLocaleManager` (6.51), androidx
`navigation-compose` (6.54).

| Dependency                         | Replacement                                                                                             | When                |
|------------------------------------|---------------------------------------------------------------------------------------------------------|---------------------|
| Android resources (`R.*`)          | Strings (6.36), UI drawables (6.39) and board assets (6.40) done; colors and raw still `R.*`            | Phase 4             |
| WorkManager                        | `expect`/`actual` `BackgroundSync`; iOS `BGAppRefreshTask`                                              | Phase 5 (see risks) |
| `NotificationUtils` (399 LOC)      | `expect`/`actual`; the board is already drawn by `renderBoard` (6.37); `notification_board.xml` stays   | Phase 5             |
| Play Billing                       | `SupporterStore` contract done (6.52); iOS needs StoreKit, or RevenueCat `purchases-kmp`                | Phase 5             |
| Play In-App Review                 | `SKStoreReviewController`                                                                               | Phase 5             |
| play-services-auth                 | in `shared/androidMain` behind `rememberGoogleSignIn` (6.53); later Credential Manager / iOS Google SDK | Phase 5             |
| Mockito, compose screenshot plugin | stay on the Android target; new `commonTest` uses fakes + Turbine                                       | No change           |

**Stays Android-only by design:** `MainActivity` (edge-to-edge, deep links, `isInForeground`),
Material You dynamic color (`expect`/`actual` since 6.42), the window theme in `styles.xml` (the
only reason
the Material Components dependency exists).

---

## 4. The two large workstreams

### 4.1 Local AI and the score estimator

These are separate problems.

**Score estimator - decided: stays C++, on iOS through `cinterop`** (6.26). A Kotlin port would
drop the NDK and make estimation testable in `commonTest`, but it is Monte Carlo (2,000 random
playouts per estimate), so parity needs a seeded bit-exact harness and Kotlin/Native speed is
unmeasured. Revisit only if the NDK becomes the cost.

**KataGo - the one hard blocker.** It runs as a child process (`ProcessBuilder` over stdin/stdout
JSON), and iOS forbids `fork`/`exec`. It must be relinked as a static library and driven
in-process, which means reverting our own patch: the upstream fork builds a library and we changed
it to emit an executable. The Eigen CPU backend we ship is portable and runs in the Simulator, so
speed is not the problem. What still stands:

- **Memory.** In-process on iOS it shares the jetsam budget with the UI (35 MB + 94 MB nets plus
  the search tree). Measure this first; it is the most likely thing to kill the feature.
- **Core topology.** `availableProcessors()` counts efficiency cores; tune with
  `activeProcessorCount`.
- **Assets.** 265 MB, so On-Demand Resources rather than bundling.
- **No build recipe in the repo.** The binaries are checked in, ARM only.

**Open decision: does iOS v1 ship without local AI?** A product call; phases 0-4 do not depend on
it. Technical recommendation: yes - local AI behind `expect`/`actual`, and in-process KataGo as its
own spike gated on the memory measurement.

### 4.2 Resources

**Strings are done (6.36).** They live in
`shared/src/commonMain/composeResources/values*/strings.xml`
in CMP format and both platforms read them through the one `Res` class
(`io.zenandroid.onlinego.resources`). CMP can't read Android escapes (`\'`, `\"`, `%%`), and
Crowdin's
Android and "Compose Multiplatform XML" exports both produce them, so a Crowdin **Custom Post-Export
Processor** (JavaScript in Project Settings -> File Processors) unescapes every exported
`strings.xml`; the GitHub integration is unchanged. `app_name` stays an Android resource because the
manifest needs it.

**UI drawables are done (6.39)**, in `composeResources/drawable/` with three `drawable-dark/`
twins, and so are the board assets (6.40), apart from the stone vectors' Android copy for
`renderBoard`. Still Android resources:

- **Colors** - CMP has no color resources. Left: the window theme's (`styles.xml`),
  and `colorTextSecondary` (notifications).
- **Platform-bound** - launcher icons, the notification icon, `notification_board.xml`
  (`RemoteViews`) and the two `.pem` files (`network_security_config.xml`). The stone sound is
  `composeResources/files/stone.wav` since 6.46.

---

## 5. Risks

| Risk                         | Notes                                                                                                                                                                                                        |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Toolchain currency**       | Kotlin 2.4.21 / AGP 9.3.3 / Gradle 9.7.1, inside the supported matrix (6.33). **CMP 1.13.0-alpha02 puts the whole Android app on Jetpack Compose 1.13.0-alpha03** (6.35); move to 1.13 stable when it ships. |
| **Room on a live schema**    | v20, 4 auto-migrations, 13 converters, and `fallbackToDestructiveMigration` silently wipes caches on a bad migration. `MoveTree` blobs written by Moshi are now read by kotlinx (6.1).                       |
| **Localization regression**  | Strings moved in 6.36 with a mechanical diff: every value in the 9 shipped locales matched Android's rendering. Watch Crowdin exports: the post-export processor must keep running.                          |
| **iOS background behaviour** | `BGAppRefreshTask` has no interval guarantee, so turn notifications will be worse than Android's 15-minute poll without server push (APNs), which OGS would have to support. No FCM today.                   |
| **KataGo memory on iOS**     | A new class of risk that does not exist on Android, where the engine has its own process.                                                                                                                    |
| **Bandwidth**                | A multi-month refactor on a personal project. Phases 0-1 are the hedge: they pay off on their own.                                                                                                           |

---

## 6. Decisions and traps still load-bearing

Only what still matters: traps, deliberate behaviour changes, and decisions that look wrong but
aren't. Slice numbers (6.x) are kept in parens so a commit still maps to an entry; the full
slice-by-slice write-up (exact test names, compose-report deltas, APK size diffs) is preserved at
`git show 9cf6cb29:docs/KMP_MIGRATION_ASSESSMENT.md` if that detail is ever needed again. Unit
tests stand at 201: 143 in `:shared` host tests (127 of them in `commonTest`, which also run on
the iOS simulator), 43 in `:shared` `iosTest` (simulator only), 15 in `:app`.

### 6.1 Data layer (Phase 3: serialization, network, storage, DI)

- **Serialization** (6.1, 6.21): one `appJson`. `explicitNulls = false`, so every nullable DTO
  property defaults to `null` - the safety net for a property added without a default.
  `isLenient` doesn't cover numbers: OGS sends `99999999.0` for an `Int?`, handled per-file by
  `@file:UseSerializers` with `LenientIntSerializer`/`LenientLongSerializer` - a new DTO file
  without it loses that leniency. `AnySerializer` reproduces Moshi's runtime types (`Double`,
  `LinkedHashMap`) because call sites cast to them. Room stayed at schema version 20 on purpose,
  so a stale `MoveTree` blob throws visibly instead of being silently wiped.
- **jsoup → `fromHtml`** (6.2, 6.43): Android-only in Compose Multiplatform 1.13, behind
  `htmlToAnnotatedString`. **The iOS `actual` is a placeholder** - line breaks only, no
  links/bold/italic. Replace before iOS ships.
- **Coil 2 → 3** (6.3): `rememberAsyncImagePainter` now defaults to `Size.ORIGINAL`, so avatar
  sites use `AsyncImage` instead (silent memory regression otherwise). `processGravatarURL` rounds
  up to the CDN's 64/128/256/512 buckets instead of always asking for 512.
- **Retrofit → Ktorfit** (6.4): `baseUrl` needs a trailing `/`, paths must not start with one,
  `@Body` needs `defaultRequest` for `Content-Type`, a `%` in a path crashes KSP (Google OAuth
  constants are `@Query` defaults), `expectSuccess` throws on 3xx (opted out for the Google
  handshake). Errors surface as `OGSApiException`. **Still owed, unit tests can't reach them:**
  seven body-bearing endpoints (`createAccount`, `openChallenge`, `challengePlayer`,
  `markPuzzleSolved`, `ratePuzzle`, `acknowledgeWarning`, `deleteAccount`) - see §1.2.
- **Cookie store** (6.5): SharedPreferences, not DataStore (`isLoggedIn()` must be synchronous on
  cold start). **Only `csrftoken` and `sessionid`, only for the OGS host** - a trust boundary; a
  third required cookie would break login with no obvious cause. The legacy Java-serialized
  import stays until Play Console shows pre-6.5 installs are negligible (§1.1).
- **Logging → Kermit** (6.6): `CrashlyticsBreadcrumbWriter` forwards Info+ as breadcrumbs and
  never records non-fatals. **Release logcat is silent** - still owed, see §1.2.
- **`java.time` → kotlin.time/kotlinx-datetime** (6.7): Room still stores epoch millis. Desugaring
  stays (kotlinx-datetime needs `java.time` below API 26). The stats chart tooltip date still uses
  `DateUtils`/English month names (kotlinx-datetime has no locale names).
- **DI** (6.8, 6.31): remaining global lookups are deliberate - lazy injection breaks cycles in
  `OGSWebSocketService`/`UserSessionRepository`, the two WorkManager workers, `GameConnection`,
  `Globals1`'s `ClockDriftRepository`. `GlobalContext` is JVM-only; common code uses
  `KoinPlatform.getKoin()`. Koin follows the layers: `sharedModule` (commonMain) →
  `androidPlatformModule`/`iosPlatformModule` (platform contract) → each app's composition root.
  `iosTest/.../SharedModuleTest` resolves every `sharedModule` definition against stubs of the
  platform contract, so a definition added without its platform binding fails there.
  `OGSWebSocketService.eventListeners` is copy-on-write over persistent collections - a listener
  removed mid-dispatch can still receive that one event (6.13).
- **Analytics/Crashlytics behind interfaces** (6.9, 6.10): all crash reports go through the common
  `reportableException()` filter - cancellation and network errors are no longer reported, HTTP
  5xx is wrapped in `ServerException`. **The iOS `CrashReporter` is still a placeholder** - logs
  through Kermit, drops breadcrumbs/user id/keys. Replace before iOS ships.
- **Layering** (6.11): nothing in `data.model`/`gamelogic` imports `R`, `android.graphics`,
  Compose UI. Presentation enums (`BoardTheme`, `AppTheme`, `AppLanguage`, `TutorialIcon`) carry no
  resource ids - a new enum entry won't compile without its resources.
- **Clock text** (6.16): an expired clock shows `0.0s` instead of `0.0`, and decimals follow the
  locale (`1,5s` in German) - behaviour changes from the Java version. **Digits are always
  Latin**, like every integer the app prints; a device in a locale with its own digits (Arabic)
  used to get them only in these decimals.
- **Websocket** (6.14): the Ktor `WebSockets` plugin's `pingInterval` is ignored by the OkHttp
  engine - the 15s ping lives on the shared `OkHttpClient` and pings REST HTTP/2 connections too;
  Darwin takes it from the plugin. **`disconnect()` closes the outbound queue rather than
  cancelling it**, so `cleanup()` messages drain before the close frame - still owed on logout,
  see §1.2. A bad incoming payload is reported and skipped instead of dropping the socket. CIO was
  rejected as the Android engine (HTTP/1.1 only, worse recovery, no emulator DNS hook).
- **Threading** (6.15): `OGSWebSocketService`/`GameConnection` share a coroutine `Mutex`; former
  `runBlocking` calls wait on `loginStatus.first()`, which can really wait at cold start - don't
  swap it for `replayCache`. **Ref-counting lives in the service** (the old re-entrant lock would
  deadlock under a plain `Mutex`); `GameConnection.close()` launches `release()` on
  `applicationScope` - still owed, see §1.2.
- **HTTP client / storage / session cluster in `commonMain`** (6.27-6.30): `HTTPConnectionFactory`
  (OkHttp, emulator DNS hook, 15s ping), cookie persistence/import and the DataStore delegates
  stay `androidMain`. `PersistenceManager` stays synchronous SharedPreferences by design (a
  DataStore move would need a migration whose failure drops the session). `UserSessionRepository`
  takes an `AppDataEraser` instead of a `Context` - Android's calls
  `ActivityManager.clearApplicationUserData()` (wipes everything, kills the process); **an iOS
  `AppDataEraser` must clear the database, both DataStores and both `NSUserDefaults` suites
  itself.** **Known bug, unchanged:** the Joseki label regex `<(.):([A-H]|[J-T]\d{1,2})>` excludes
  columns A-H with two-digit ranges; the intended pattern is `[A-HJ-T]\d{1,2}` - fixing it changes
  what the explorer shows, so it wasn't fixed as part of the move.
- **Room in `commonMain`** (6.25): schema/version unchanged (byte-identical `20.json`, same
  identity hash - an older APK still opens the file). Android keeps the framework SQLite; iOS gets
  `BundledSQLiteDriver`. Every DAO call became `suspend`/`Flow` (native targets reject blocking
  DAOs). `Database` in `Database.kt` must spell out the `expect object DatabaseConstructor` in
  full, or KSP fails.
- **Score estimator on iOS** (6.26): **decided - stays C++, via `cinterop`**, not a Kotlin port
  (Monte Carlo, 2000 playouts/estimate - parity would need a seeded bit-exact harness,
  Kotlin/Native speed unmeasured). JNI symbol is `Java_..._gamelogic_NativeEstimator_estimate`;
  **renaming or moving that class breaks scoring silently** (`UnsatisfiedLinkError` is caught and
  only logged) - R8 keeps it through the default native-method rule. `iosTest` runs the real C++
  on `Dispatchers.Default`, whose 512KB iOS stacks are the tightest the estimator's stack-allocated
  grids will see.
- **iOS platform module** (6.32): `getLong` must read `objectForKey`, not `integerForKey` (which
  returns 0 for a missing key, not the default). `SessionCookiePersistence` stores the whole
  cookie map as one JSON string per write. NSURLSession's own redirect-following is blocked by
  Ktor's delegate, so `followRedirects = false` still holds for the Google handshake.
  **Kotlin/Native's standalone simulator test run fails every TLS handshake** - real-network iOS
  tests need a booted, non-standalone simulator.

### 6.2 Resources (Phase 4)

- **Strings** (6.36): one CMP-format string set read by both platforms. **Crowdin's Android and
  "Compose Multiplatform XML" exports both produce Android escapes (`\'`, `\"`, `%%`) that CMP
  shows literally** - a Custom Post-Export Processor unescapes every exported `strings.xml`. **If
  the processor stops running, the next Crowdin PR silently breaks translated strings.** `app_name`
  stays an Android resource (the manifest needs it). A partially-translated device locale now
  falls back to English per-string instead of failing to load.
- **Drawables** (6.39, 6.40): **CMP's vector parser only accepts literal `#` colors** - an
  `@color/`/`?attr/` reference builds fine and throws at load time. **The dark-mode qualifier
  follows the system setting, not the app's own theme setting**, unlike before - affects any
  `Image` using a `drawable-dark/` twin (the Tsumego arrows were swapped for tinted `Icon`s to
  dodge this). The board's text is centered on ink bounds measured per-platform
  (`Paint.getTextBounds` on Android, Skia's `Font.measureText` on iOS); **the iOS typeface must be
  `.AppleSystemUIFont`** - Helvetica is a couple of px off on some digits. Stone shadows are a
  radial gradient fitted to Android's old shadow layer, not a per-platform `Paint`/`Build` check.
- **`BoardView` removed** (6.37): notifications render through `renderBoard` (Android-only,
  rasterizes stone drawables to bitmaps since `VectorPainter` needs a composition). Notifications
  now use the Settings board style and system dark mode instead of always light wood.
- **Material icons** (6.41): multiplatform artifact is frozen at androidx 1.7.8 parity and
  compiled against Compose 1.7 - works on newer Compose only through binary compatibility;
  re-check after every Compose upgrade.

### 6.3 UI → Compose Multiplatform (Phase 4)

- **ViewModel plumbing** (6.42): Molecule runs `RecompositionMode.Immediate` on `viewModelScope`
  (no frame clock is available without `AndroidUiDispatcher`). **Behaviour change:** state
  recomputes on every change instead of at most once per frame.
- **Orientation/window size** (6.44, 6.45): `isLandscape()`/`screenSize()` keep Android on
  `LocalConfiguration` and use window size only on iOS - a window-size check was rejected on
  Android because `containerSize` can be zero on the first frame.
- **Puzzle picker dropdown** (6.20): kept its own lazy `Popup`-based menu instead of Material3's
  `ExposedDropdownMenu`, which still lays out a scrolling `Column` and would render every puzzle
  in the collection on open.
- **Sound** (6.46): the stone sound is an uncompressed WAV in Compose resources - **AGP never
  compresses `.wav`, which `AssetSoundLoader.openFd` depends on; a compressed asset would throw.**
- **Review prompt / notification cancel** (6.46): `ReviewRequester` and `GameNotificationCanceller`
  are platform-contract interfaces; the Android `MainActivity` binds them, **iOS still has no
  implementation** (needs `SKStoreReviewController`) - not reproducible for review prompts on the
  dev account either way (Play quota), see §1.2.
- **Local AI** (6.47): `AnalysisEngine` is a platform contract; iOS binds
  `UnavailableAnalysisEngine`, whose `start()` throws, so the AI-game screen shows an error state
  instead of crashing. **The open decision in §4.1 (ship iOS v1 without local AI) isn't made -
  this only records today's fallback**, and the iOS app still needs to hide the AI entry point if
  it ships without it.
- **Joseki markdown** (6.48): renderer uses the CommonMark flavour, not GFM (no bare-URL
  autolinking, no tables). Link labels ("(video)"/"(external link)") are only applied to
  **inline** markdown links - reference links and autolinks render unlabeled.
- **Charts** (6.49, 6.50): MPAndroidChart was replaced by a plain Compose `Canvas` (`RankChart`)
  with no axis labels, legend or pinch-zoom - deliberately dropped, nothing used them. Best-streak
  /highest-rank dates are still English-only (`MonthNames.ENGLISH_ABBREVIATED`).
- **Settings - language** (6.51): `AppLanguageSetting` is a platform contract. Below Android 13,
  `MainActivity` recreates the activity itself instead of Settings, and only when the locale
  actually changed (avoids a double recreate after a phone-level language change). **iOS picks up
  a new language only on next launch - the iOS app owes a "restart to apply" notice.**
- **Supporter billing** (6.52): `SupporterStore` is a platform contract (plain
  `SupporterProduct`/`SupporterPurchase`, no Play Billing types in the ViewModel). **iOS binds
  `UnavailableSupporterStore`** - needs StoreKit or RevenueCat's `purchases-kmp` before iOS ships.
  Errors show as a `Snackbar` instead of a `Toast` now (a null message falls back to the
  exception's `toString()`). Verified on a production build: tiers/prices, the subscribe and
  billing flow, upgrade/downgrade and the manage-subscription link - see §1.2.
- **Onboarding - Google sign-in** (6.53): `rememberGoogleSignIn` is a platform contract returning
  `Success`/`Failure`/`Cancelled`. **iOS always reports `Cancelled`** for now - a real iOS
  implementation is still owed before ship. Verified on a release build - see §1.2.
- **Navigation host** (6.19, 6.54): routes are a `@Serializable sealed interface` (`Route.kt`);
  **each `@SerialName` is also the Firebase `screen_view` name and the deep-link shape** -
  renaming one changes both. Compose Multiplatform 2.10's predictive-back transitions changed
  from the nav host's pop transitions to the platform default; `NavHost` now sets the old 500ms
  fades explicitly. `OnlineGoApp` takes `versionCode`/`onForceReviewPrompt`/`onTestNotification`/
  `onLaunchBillingFlow`/`onFinish` as parameters instead of reaching for an `Activity`.
- **App shell** (6.55): `AppViewModel`/`OnlineGoRoot` (commonMain) hold theme, onboarding flag,
  login status, and socket connect/disconnect on resume/pause. `SystemBarsAppearance` is an
  expect/actual - on iOS it sets the status-bar style on the root view controller (6.57).
  WorkManager's `*_WORK_NAME` constants were kept (unlike the matching `*_PERIOD_MINUTES` ones)
  because they're still needed to cancel work scheduled by old installs (6.18).
  `REVIEW_PROMPT_RATED` is kept even though its only readers were deleted (6.56) -
  `shouldPromptForReview` still reads it from existing installs. **Still Android-only by design:**
  locale recreate below API 13, StrictMode, notification channels/WorkManager schedule,
  `isInForeground` (read by the notification workers - still owed, see §1.2), Koin start, the
  Crashlytics breadcrumb writer, Ktor's `SecureRandom` workaround below API 26.

### 6.4 iOS app (Phase 5)

- **Shell** (6.57): `iosApp/` is a UIKit app with `AppDelegate`/`SceneDelegate`, not SwiftUI.
  The root view controller has to own `preferredStatusBarStyle`, and a SwiftUI host doesn't
  forward it to a hosted `UIViewController`. `MainViewController(versionCode:)` (iosMain) wraps
  `ComposeUIViewController` in a parent `OnlineGoViewController`, which `SystemBarsAppearance`
  reaches via `LocalUIViewController.current.parentViewController`. The
  `ComposeUIViewControllerDelegate` route is deprecated. It uses the UIScene lifecycle because Apple
  makes it mandatory after iOS 26.
  `Shared` is a static framework built by `embedAndSignAppleFrameworkForXcode`. `-lc++` is in
  `OTHER_LDFLAGS` because a static framework doesn't carry the estimator `.def`'s `linkerOpts`.
  `tutorials.json` is referenced from `app/src/main/assets`, not copied. The `.xcodeproj` uses
  folder-synchronised groups (object version 77), so it needs Xcode 16 or later.
  **Known build warning:** Coil 3.6.3 is built against Skiko 0.150.1 and CMP 1.13.0-alpha02 pulls
  0.153.0. It runs, but recheck when either is upgraded.

### 6.5 Toolchain

- **Versions** (6.33-6.35): Kotlin 2.4.21, AGP 9.3.3, Gradle 9.7.1 - the newest patch releases
  inside Kotlin 2.4.21's supported matrix (AGP 9.3 refuses Gradle below 9.5; AGP 9.4 is outside
  the 2.4.21 range). KSP 2.3.12. **Material3 is pinned to 1.9.0**
  (`composeMultiplatformMaterial3`), independent of the CMP version, so Android keeps material3
  1.4.0 exactly as before. **CMP 1.13 is still alpha** - needed for three bugs fixed only there (a
  `stringResource` main-thread ANR, a blocking/suspend-read deadlock, a crash on CLDR-missing
  plural locales); move to 1.13 stable when it ships. CMP 1.13 requires `compileSdk` 37.1 and
  `minSdk` 24 (dropped Android 6.0).
- **`:shared` module setup** (6.22, 6.23): applies the Compose compiler plugin even though it has
  no composables yet - without it, model classes compile as external/unstable (watch `unstable`
  parameter counts in the compose report, not skippable totals, since strong skipping hides this).
  Uses `com.android.kotlin.multiplatform.library`, not `com.android.library` + KMP (that
  combination is deprecated in AGP 9, removed in AGP 10) - **it has no
  `externalNativeBuild`/`BuildConfig`**, so the estimator's JNI glue and `CMakeLists.txt` stay in
  `:app`.

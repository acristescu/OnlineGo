# Kotlin Multiplatform migration

Goal: move OnlineGo to Kotlin Multiplatform with **iOS as the second target** and **Compose
Multiplatform as the end state**, without a long-lived branch - **the Android app builds and ships
at the end of every phase.**

**Verdict: feasible.** The UI is 100% Compose (one XML layout, a notification `RemoteViews`), there
is no RxJava, no `Parcelable`, and Koin, Molecule, Room, DataStore, `ViewModel`, Turbine and
kotlinx-collections-immutable are already multiplatform. The cost is dominated by resources
(663 strings x 16 locales) and the local-AI engine, which cannot run on iOS as built today.

**Status.** Thirteen Phase 0 slices are merged (section 6). None of them has shipped in a release
yet,
and some still need on-device checks (section 1.2). Phases 1-5 have not started.

The original assessment, with the full write-up of each finished slice, is at
`git show d529259:docs/KMP_MIGRATION_ASSESSMENT.md`.

---

## 1. What is left

### 1.1 Phase 0 - de-coupling (Android-only, no KMP tooling)

Worth doing even if the migration stops here.

- [ ] **Localize the clock text in `utils/Globals.kt`.** The file no longer imports anything
  Android-only (6.12), but `computeTimeLeft` -> `formatMillis` -> `plural` hand-build English
  (`"%d day%s"`, `"+ ... / move"`), shown untranslated in the game clocks and game-list timers in
  every locale. `computeTimeLeft` should return numbers and the UI should format them from
  resources. That also removes `String.format`, which is JVM-only.
- [ ] **Replace the JVM threading primitives in `data`** (6.13 did the collections, atomics, `UUID`
  and `Locale`): `synchronized` / `@Synchronized` in `OGSWebSocketService`, `GameConnection`,
  `ActiveGamesRepository` and `FinishedGamesRepository`; `thread` / `Thread.sleep` /
  `runBlocking` in `OGSWebSocketService`; `TimeUnit` in `ReviewPromptRepository`;
  `System.currentTimeMillis` in six `data` files. The websocket ones go with its Ktor port in
  Phase 3 - they are tangled with OkHttp's listener threads, and `connectToGame` would have to
  become `suspend` to take a `Mutex`.
- [ ] **Build the `@file:UseSerializers` CI check** - every `data/model` file declaring a
  `Boolean` / `Int` / `Long` must carry the matching annotation. Without it, a new DTO file
  silently loses the lenient decoding (6.1).
- [ ] **Delete dead weight:** `buildFeatures { viewBinding = true }`, the empty `fileTree("libs")`
  dependency, `res/anim/`, `rang.hpp`, the unused `NOT_CHARGING_PERIOD_MINUTES` constants, and
  `FacebookLoginCallbackActivity` (disabled in the manifest *and* at runtime). Rename - do not
  delete - `ui/screens/game_legacy/GamePresenterTest.kt`; it still covers live code.
- [ ] **Type-safe navigation routes:** the 13 string routes in `Navigation.kt` to `@Serializable`
  types. Also drops the hardcoded bottom-bar `listOf("myGames", "learn", "stats", "settings")`.
- [ ] **Re-test Material3's `ExposedDropdownMenu`.** If its `LazyColumn` performance problem is
  fixed, `ui/composables/ScrollableDropDownMenu.kt` (671 LOC, raw `PopupWindow`, cannot be
  ported) can be deleted instead of rewritten.
- [ ] *Optional tidy:* add `= null` to the 129 nullable-without-default DTO properties. Behaviour is
  already covered by the `Json` config.

**Owed after a release ships:** delete the legacy `CookiePersistence` prefs file, the
`SerializableCookie` shim and the import branch (6.5). They are the rollback path until then.

**Product call:** raising `minSdk` to 26 is the only way to drop core library desugaring (6.7).

### 1.2 On-device checks owed for merged work

Verified on device: the session survives an in-place upgrade, password login, Google sign-in, and
declining a challenge (an unsafe request, so CSRF works).

Still owed - unit tests cannot reach these:

- [ ] **The other seven body-bearing endpoints.** All eight broke at once during the Ktorfit port on
  a missing `Content-Type`; login proves the fix, but these have not sent a real request since:
  `createAccount`, `openChallenge`, `challengePlayer`, `markPuzzleSolved`, `ratePuzzle`,
  `acknowledgeWarning`, `deleteAccount`.
- [ ] **Crashlytics breadcrumbs** arrive as `I/Tag: message`, and release logcat is silent.

### 1.3 Phases 1-5

| Phase                                | Work                                                                                                                                                                                                                                                                                                                   | Estimate                        |
|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| **1 - extract `:shared`**            | One library module holding `model`, `logic`, `network`, `database`, `data`. `:app` keeps the Activity, notifications, WorkManager, billing, Play services and UI. Surfaces every dependency cycle before KMP can muddy the diagnosis.                                                                                  | a few days                      |
| **2 - make `:shared` multiplatform** | `androidTarget` + `iosArm64` / `iosSimulatorArm64` / `iosX64`; everything starts in `androidMain`. Move `model` + `logic` (~950 LOC) to `commonMain` and `RulesManagerTest` to `commonTest` (expand it). Decide the score estimator (5.1).                                                                             | 1-2 weeks (+1-2 for estimator)  |
| **3 - data layer to `commonMain`**   | Room KMP (bundled SQLite driver, `expect`/`actual` DB path), DataStore on okio `Path` for its three stores, Ktor engine per platform, repositories one file at a time. Session persistence is already behind `SessionCookiePersistence`. The websocket layer moves once the JVM atomics are gone.                      | 4-6 weeks                       |
| **4 - Compose Multiplatform**        | UI into `:shared/commonMain`. Resources (4.2), icons, chart, markdown, `ScrollableDropDownMenu`, Material You `expect`/`actual`. `BoardComposable` has four Android leaks, all with direct CMP equivalents: `nativeCanvas.drawText`, `MotionEvent` / `pointerInteropFilter`, `android.graphics.Rect`, `colorResource`. | 6-10 weeks (resources dominate) |
| **5 - iOS app**                      | SwiftUI shell + `ComposeUIViewController`, notifications, background refresh, billing, Google sign-in, App Store plumbing. Bitrise needs a macOS stack.                                                                                                                                                                | 4-8 weeks, excluding local AI   |

Estimates are order-of-magnitude, single developer. The one data point so far: the serialization
slice came in on time only because the golden-JSON corpus was dropped. That moved verification out
of CI and into on-device testing, where its one real bug surfaced. Budget for where verification
happens, not just for writing the code.

---

## 2. Target architecture: three modules

```
:shared    KMP library - model, logic, network, database, data, and (from phase 4) UI
:app       Android application - MainActivity, notifications, WorkManager, Play Billing,
           Play Review, Google sign-in
iosApp/    Xcode project - SwiftUI shell, BGTaskScheduler, StoreKit, UNUserNotificationCenter
```

Layering inside `:shared` lives in **package names**, and `commonMain` / `androidMain` / `iosMain`
do the job a module split would: the compiler rejects an `android.*` import in `commonMain`.
Migration is then "move this file to `commonMain`", always compiling.

A nine-module `:core:*` split is the default advice and wrong here. There is no `build-logic`, so
it would mean nine hand-kept build files against a brand-new toolchain (Kotlin 2.3.20, AGP 9.2.1).
Compose resources generate one `Res` per module, which would fragment the largest workstream.
`model` + `logic` is 950 LOC, which is a package, not a module. And 197 of the last 199 commits
have one author.

Revisit when clean-build time becomes a real problem (record a baseline first), when a second
regular contributor appears, or when local AI needs Play Feature Delivery for its 265 MB of assets.

---

## 3. Dependencies

### 3.1 Status

| Dependency                                      | Replacement                                                                                                    | Status                                                         |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| Moshi (reflective), `org.json`                  | kotlinx.serialization, one `appJson`                                                                           | **Done** - 6.1                                                 |
| jsoup                                           | `AnnotatedString.fromHtml()`                                                                                   | **Done** - 6.2                                                 |
| Coil 2                                          | Coil 3 + `coil-network-okhttp`                                                                                 | **Done** - 6.3                                                 |
| Retrofit                                        | Ktorfit over Ktor 3                                                                                            | **Done** - 6.4                                                 |
| PersistentCookieJar, `gms IOUtils`              | `OGSCookieStore` behind Ktor `HttpCookies`                                                                     | **Done** - 6.5                                                 |
| `android.util.Log`, `FirebaseCrashlytics.log`   | Kermit                                                                                                         | **Done** - 6.6                                                 |
| `java.time`, `SimpleDateFormat`, `Date`         | `kotlin.time` + kotlinx-datetime                                                                               | **Done** - 6.7                                                 |
| OkHttp (direct)                                 | Android Ktor engine only                                                                                       | Mostly done; the websocket still takes `OkHttpClient` directly |
| Firebase Analytics                              | Injected `Analytics`; becomes a common interface, iOS implementation in Swift                                  | **Done** - 6.9                                                 |
| Firebase Crashlytics (non-log)                  | Global `CrashReporter`; `expect`/`actual` in Phase 2, iOS via Firebase iOS SDK or GitLive                      | **Done** - 6.10                                                |
| `java.util.concurrent`, `UUID`, `Stack`, ...    | stdlib atomics, persistent collections, `kotlin.uuid`, `ArrayDeque`                                            | **Done** - 6.13; `synchronized` / threads remain (1.1)         |
| `BuildConfig`                                   | BuildKonfig                                                                                                    | To do - Phase 2                                                |
| Koin Android artifacts                          | `koin-core` + `koin-compose` + `koin-compose-viewmodel`; drop `androidApplication()`                           | To do - Phase 2                                                |
| Molecule `AndroidUiDispatcher` / `ContextClock` | multiplatform frame clock; extract the shared `moleculeScope` base from the two ViewModels                     | To do - Phase 2                                                |
| Room 2.8, DataStore 1.2                         | same libraries, KMP setup                                                                                      | To do - Phase 3                                                |
| MPAndroidChart                                  | Vico 2.x / KoalaPlot / Compose `Canvas` (`ChartWrapper.kt`, 416 LOC)                                           | To do - Phase 4; already confined to `ChartWrapper.kt`         |
| Markwon                                         | `multiplatform-markdown-renderer-m3` (`JosekiExplorerUI`, ~90 LOC)                                             | To do - Phase 4                                                |
| `material-icons-extended` (109 refs, 21 files)  | vendored `ImageVector`s                                                                                        | To do - Phase 4                                                |
| navigation-compose (androidx)                   | `org.jetbrains.androidx.navigation`                                                                            | To do - Phase 4                                                |
| Android resources (`R.*`)                       | Compose Multiplatform resources - see 4.2                                                                      | To do - Phase 4                                                |
| `AppLocaleManager`                              | `expect`/`actual`; iOS writes `AppleLanguages` and needs a restart                                             | To do - Phase 4                                                |
| WorkManager                                     | `expect`/`actual` `BackgroundSync`; iOS `BGAppRefreshTask`                                                     | To do - Phase 5 (see risks)                                    |
| `NotificationUtils` (348 LOC)                   | `expect`/`actual`; re-rasterize the board with Compose, deleting `BoardView` (737 LOC) and the last XML layout | To do - Phase 5                                                |
| Play Billing                                    | StoreKit, or RevenueCat `purchases-kmp`                                                                        | To do - Phase 5                                                |
| Play In-App Review                              | `SKStoreReviewController`                                                                                      | To do - Phase 5                                                |
| play-services-auth                              | Credential Manager / iOS Google SDK; the token exchange is already plain REST                                  | To do - Phase 5                                                |
| Mockito, compose screenshot plugin              | stay on the Android target; new `commonTest` uses fakes + Turbine                                              | No change                                                      |

### 3.2 Stays Android-only by design

`MainActivity` (edge-to-edge, deep links, `isInForeground`), Material You dynamic color (wrap in
`expect`/`actual`), the two window themes in `styles.xml` (the only reason the Material Components
dependency exists), and `ScrollableDropDownMenu` unless Material3 makes it redundant.

---

## 4. The two large workstreams

### 4.1 Local AI and the score estimator

These are separate problems.

**Score estimator (`libestimator.so`).** The JNI surface is one function, `RulesManager.estimate()`,
a clean `expect`/`actual` boundary. Options: cross-compile for iOS with `cinterop`; **port
`Goban.cpp` (~1,030 LOC of real logic) to Kotlin - recommended**, since it removes the NDK, makes
estimation testable in `commonTest` and deletes ~1,500 LOC of dead C++ (benchmark first); or use
OGS's server-side estimate on iOS, at the cost of a round-trip on a latency-sensitive action.

**KataGo - the one hard blocker.** It runs as a **child process** (`ProcessBuilder` over
stdin/stdout JSON), and iOS forbids `fork`/`exec`. It must be relinked as a static library and
driven in-process. That means reverting our own patch: the upstream fork already builds a library,
and we changed it to emit an executable. The Eigen CPU backend we already ship is portable, needs no
tuner and runs in the Simulator, so performance is not the problem. What still stands:

- **Memory.** In-process on iOS it shares the jetsam budget with the UI (35 MB + 94 MB nets plus the
  search tree). Measure this first; it is the most likely thing to kill the feature.
- **Core topology.** `availableProcessors()` counts efficiency cores, so thread count needs
  `activeProcessorCount` tuning.
- **Assets.** 265 MB, so On-Demand Resources rather than bundling.
- **No build recipe in the repo.** The binaries are checked in, ARM only.

**Open decision: does iOS v1 ship without local AI?** It is a product call; phases 0-4 do not
depend on it. Technical recommendation: yes - local AI behind `expect`/`actual`, and in-process
KataGo as its own spike gated on the memory measurement.

### 4.2 Resources

663 strings, 16 locale folders (`ro` 658 translated down to 7 empty stubs), 456 `stringResource`
sites in 39 files, 33 vectors, 3 board JPEGs, 1 `.ogg`. Compose Multiplatform resources read the
same
`strings.xml`, so Crowdin needs only a path change. The seam already exists: `TextResource` and the
`labelResId` / `DetailValue.Resource` idiom carry ~130 resource ids as `Int` in ViewModel state,
which become typed `StringResource`s mechanically (`GameViewModel` ~30, `OnboardingViewModel` ~25,
`AiGameViewModel` ~20, `MyGamesViewModel` ~18, `FaceToFaceViewModel` ~15). The non-mechanical parts
are `ui/screens/game/TimeControlDescription.kt` (`Resources.getQuantityString`) and the clock text
still built in `Globals.kt` (1.1). Do it against one module so there is one `Res` class.

---

## 5. Risks

| Risk                         | Notes                                                                                                                                                                                      |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Toolchain currency**       | Kotlin 2.3.20 / AGP 9.2.1 are very new. Verify KMP plugin, CMP, Room KMP, Ktorfit and the configuration cache together before Phase 2.                                                     |
| **Room on a live schema**    | v20, 4 auto-migrations, 13 converters, and `fallbackToDestructiveMigration` silently wipes caches on a bad migration. `MoveTree` blobs written by Moshi are now read by kotlinx (6.1).     |
| **Localization regression**  | 663 strings x 16 locales moved wholesale; needs a mechanical diff, not eyeballing.                                                                                                         |
| **iOS background behaviour** | `BGAppRefreshTask` has no interval guarantee, so turn notifications will be worse than Android's 15-minute poll without server push (APNs), which OGS would have to support. No FCM today. |
| **KataGo memory on iOS**     | A new class of risk that does not exist on Android, where the engine has its own process.                                                                                                  |
| **Bandwidth**                | A multi-month refactor on a personal project. Phases 0-1 are the hedge: they pay off on their own.                                                                                         |

---

## 6. Done - what to know about each finished slice

Only what is still load-bearing: traps, deliberate behaviour changes, and decisions that look wrong
but are not. Unit tests stand at 106.

### 6.1 Moshi to kotlinx.serialization (+ `org.json`)

- One `appJson` (`utils/AppJson.kt`). Each flag reproduces a Moshi behaviour: `ignoreUnknownKeys`,
  `explicitNulls = false` (what let 129 nullable properties work unedited; an explicit `null` stays
  `null` rather than taking the default - pinned in `NullableDefaultsTest`), `encodeDefaults`,
  `isLenient`.
- **`isLenient` does not cover numbers.** OGS sends `"order": 99999999.0` for an `Int?`, which
  crashed the puzzle directory on device. `LenientIntSerializer` / `LenientLongSerializer` accept
  exactly-integral floats, applied per file through `@file:UseSerializers` - hence the CI check
  in 1.1.
- `AnySerializer` reproduces Moshi's runtime types (number to `Double`, object to `LinkedHashMap`)
  because call sites cast to them.
- kotlinx serializes class-body properties: `OGSClock.receivedAt` and `AiGameState.chatText` are
  `@Transient`.
- The Room version was **deliberately left at 20**, so a stale `MoveTree` blob throws visibly
  instead of being wiped. The stack trace points at the DAO, not at a decode boundary.

### 6.2 jsoup

Replaced by `AnnotatedString.fromHtml()` at the render site in `TsumegoUI`. Descriptions now keep
formatting, links and line breaks instead of being flattened. This moved an Android-only API from
the
ViewModel to the UI layer, where it needs an `expect`/`actual` or the markdown renderer at Phase 4.

### 6.3 Coil 2 to Coil 3

- It forced **compileSdk 37 and Compose 1.12**. Coil 3.4.0 would have worked without either;
  3.6.3 was a choice. `targetSdk` stayed at 36.
- `rememberAsyncImagePainter` now defaults to `Size.ORIGINAL`, so those sites moved to `AsyncImage`.
  Without that, the regression is silent memory use, not a compile error.
- `processGravatarURL` always requested 512 px - `2^(log2 w)` is `w`, then floored at 512. It now
  rounds up to the CDN's 32/64/128/256/512 buckets. Gravatar-hosted avatars still use exact sizes,
  so they do not share cache entries across screens.
- Left as is: the home header loads at 56dp but draws at 64dp; the 124dp Stats avatar is the only
  512 bucket (crossfade kept there); another player's Stats page waits on the profile call.

### 6.4 Retrofit to Ktorfit

Traps, all found at runtime:

- `baseUrl` needs a trailing `/` and paths must not start with one.
- `@Body` sets no `Content-Type`; `defaultRequest { contentType(...) }` fixes it.
- A `%` in a path crashes the KSP processor, so the Google OAuth constants are `@Query` defaults.
- Ktor follows redirects itself, so both OkHttp and Ktor have `followRedirects = false`.
- `expectSuccess` throws on 3xx; the Google handshake opts out via `@ReqBuilder`.
- Errors surface as `OGSApiException` with `httpStatusCode` / `httpErrorBody` accessors. The body
  is captured eagerly because catch sites are not `suspend`.

Two pre-existing bugs fixed: the Glicko2 TSV parser dropped the oldest game, and
`acknowledgeWarning` sent a JSON string instead of `{"accept":true}`.

### 6.5 PersistentCookieJar to `OGSCookieStore`

- **SharedPreferences, not DataStore**, because `isLoggedIn()` is synchronous on the cold-start
  path. Hidden behind `SessionCookiePersistence`, the seam the KMP port cuts along.
- **Only `csrftoken` and `sessionid` are kept, and only for the OGS host.** The host check is a
  trust boundary. If OGS ever needs a third cookie, login breaks with no obvious cause - look at
  `KEPT_COOKIES`.
- The legacy import decodes the old library's Java-serialized blob. The shim must keep
  `serialVersionUID` and the **simple class name `SerializableCookie`** (`ObjectStreamClass`
  compares simple names). It survives R8 under the existing `data.**` keep rule.
- The old `CookiePersistence` file is kept on purpose as the downgrade path; contraction is owed
  (1.1).
- The websocket (`wsp.online-go.com`) never received these host-only cookies; it authenticates over
  the socket.

### 6.6 Logging to Kermit

- Writers are set in `OnlineGoApplication.onCreate`. `CrashlyticsBreadcrumbWriter` forwards
  **Info+** as breadcrumbs in all builds and never records non-fatals. Logcat is **debug builds
  only**, and release sets minimum severity Info.
- Verbose/Debug calls use the lambda overload, so release never builds those strings.
- `kermit-crashlytics` was rejected: it records a non-fatal for every `Logger.e` with a throwable.
- Old Crashlytics breadcrumbs became `i`/`w`/`e` and old `Log.v`/`d` stayed `v`/`d`, so the split
  between what reaches Crashlytics and what does not is unchanged. `BoardCellDragged` is no longer
  logged.

### 6.7 `java.time` to `kotlin.time` + kotlinx-datetime

- Instants use stdlib `kotlin.time.Instant` / `Clock` (stable, no opt-in). Zones and formatting use
  kotlinx-datetime 0.8.0. Room still stores epoch millis.
- **Desugaring stays.** kotlinx-datetime is built on `java.time` on the JVM and needs it below
  API 26.
- `OGSDateTimeTest` uses `java.time` as an oracle: query timestamps and serialized instants are
  byte-identical to before.
- Fixed: the stats chart treated local wall-clock time as UTC, skewing its windows by the user's
  offset. Changed: the chart tooltip date uses `DateUtils` (locale-ordered, e.g. "Jan 5, 2024" in
  the US), because kotlinx-datetime has English month names only.

### 6.8 Constructor injection

- `OnlineGoApplication.instance` is gone; `KataGoAnalysisEngine`, `NotificationUtils`,
  `PersistenceManager` and `WhatsNewUtils` are Koin singletons instead of objects.
- **The remaining `GlobalContext` sites are left on purpose.** Koin 4's `GlobalContext`, `get()` and
  `inject()` are in `koin-core` common code, so none of them blocks KMP:
  - `OGSWebSocketService` (`List<SocketConnectedRepository>`) and `UserSessionRepository` (socket
    and
    REST services) inject lazily to break dependency cycles. Constructor injection would need
    `Lazy<>`
    parameters.
  - `CheckNotificationsTask` and `SynchronizeGamesWork` stay in `:app` and cannot take constructor
    parameters without a `WorkerFactory`.
  - `GameConnection` is not built by Koin; `Globals1.kt` holds a top-level `ClockDriftRepository`.
- `toastException` in `Globals.kt` had no callers and is deleted (6.12).

### 6.9 Firebase Analytics behind `Analytics`

- `utils/Analytics.kt` wraps `FirebaseAnalytics` and is the only file besides `Modules.kt` that
  imports it. Parameters are a `Map<String, String?>` instead of a `Bundle`.
- In Phase 2 it becomes a common interface. The iOS implementation can be written in Swift against
  the Firebase iOS SDK and registered with Koin, so no cinterop is needed.
- `FirebaseAnalytics.Event.SIGN_UP` / `LOGIN` became the literals `"sign_up"` / `"login"`, which are
  the same values. Event names are otherwise unchanged.
- GitLive was not adopted: the whole surface is one `logEvent`. Revisit it alongside Crashlytics.

### 6.10 Firebase Crashlytics behind `CrashReporter`

- `utils/CrashReporter.kt` holds a global `CrashReporter` object (`recordException`, `log`,
  `setUserId`, `setCustomKey`, `sendUnsentReports`) and the Kermit breadcrumb writer. It is the only
  file that imports `FirebaseCrashlytics`. In Phase 2 it becomes `expect object`.
- **Global, not injected, on purpose.** `RulesManager`, `BoardComposable`'s draw code and
  `TextResource.resolve()` cannot take constructor parameters, and crash reporting is one per
  process, like Kermit's `Logger`.
- **Behaviour change:** the 10 calls that used to go straight to Crashlytics now go through the
  network-error filter. Cancellation and network errors from those sites are no longer reported, and
  HTTP 5xx is wrapped in `ServerException`. The `GameUI` and `ReviewPromptManager` catch blocks
  caught `Exception`, so they used to report `CancellationException`.
- `RulesManager` stays an `object`: its rules functions are called from `Position`'s companion and
  top-level helpers. Its only state is the native estimator; pulling that out as a `ScoreEstimator`
  belongs with the estimator decision (4.1).

### 6.11 Layering

Nothing in `data.model` or `gamelogic` imports `R`, `android.graphics`, Compose UI or anything under
`ui` any more.

- `Variation` and `UserSettings` moved from ViewModel files into `data.model`.
- Stats chart data is `Pair<Float, Float>`; only `ChartWrapper.kt` imports MPAndroidChart.
- `BoardTheme`, `AppTheme`, `AppLanguage` and `TutorialIcon` no longer carry resource ids or Compose
  `Color`. Their persisted or serialized forms (enum names, `storedValue`) are unchanged.
  - `BoardTheme`'s eight presentation fields are in `ui/theme/BoardThemeStyle.kt`, reached through
    `BoardTheme.style`.
  - The single-field ones are private extension properties next to their only consumer:
    `displayNameResId` in `SettingsUI.kt`, `TutorialIcon.resId` in `LearnUI.kt`.
  - All of them are exhaustive `when`s, so a new enum entry does not compile without its resources.
    Accessing them through an extension property disables smart casts.
- The unused `PuzzleDirectoryAction` / `TsumegoAction` (which carried `android.graphics.Point`) are
  deleted.
- Still Android-bound in `data`, but Phase 3 work rather than layering: `Context` in five
  repositories and `android.os.Build` in `HTTPConnectionFactory`.

### 6.12 `Globals.kt` split

- `utils/Globals.kt` no longer imports anything Android-only: no `Resources`, `R`, `Toast`,
  `BuildConfig`, Koin or `java.*`. It still has `String.format`, which goes with the clock-text item
  (1.1).
- `timeControlDescription` and its `formatSeconds` / `Resources.duration*` helpers moved to
  `ui/screens/game/TimeControlDescription.kt`, next to their only caller, `GameUI`. `formatSeconds`
  is now private.
- `toastException` had no callers and is deleted.
- `Pattern` became Kotlin `Regex` (same patterns; `matchEntire` is `matches()`).
  `ProcessGravatarURLTest` pins the gravatar and CDN rewriting and passes against both versions.
  `Math.floorDiv` / `floorMod` / `ceil` became `Long.floorDiv` / `Long.mod` / `kotlin.math.ceil`.

### 6.13 JVM collections, atomics, `UUID` and `Locale`

- Atomics are `kotlin.concurrent.atomics`, still experimental in Kotlin 2.3: the opt-in is
  module-wide in `app/build.gradle.kts`. `incrementAndFetch` / `update` are extension functions and
  need their own imports.
- `OGSWebSocketService.eventListeners` is an `AtomicReference` to a persistent map of persistent
  lists, updated copy-on-write, instead of a `ConcurrentHashMap` of lists locked one by one.
  Dispatch now iterates a snapshot without a lock, so a listener removed mid-dispatch can still
  receive that one event; `trySend` on its closed channel drops it.
- `ClockDriftRepository` replaced a fresh `AtomicLong` per pong with `@Volatile` longs. Latency and
  drift were never written as a pair, before or after.
- `TsumegoState.nodeStack` was a `java.util.Stack` mutated in place inside an immutable state; it is
  now a `List` and every push builds a new one.
- `AppLanguage` no longer holds a `Locale`; `fromLocaleTag` compares language subtags as strings
  (pinned in `AppLanguageTest`), and `AppLocaleManager` builds the `Locale` it needs.
  `uppercase(Locale.ROOT)` / `capitalize(Locale.UK)` became the locale-invariant stdlib
  `uppercase()` / `replaceFirstChar { it.titlecase() }`.
- Left alone: `KataGoAnalysisEngine`'s process plumbing (it cannot run on iOS as is, 4.1) and the
  `UUID` in `ScrollableDropDownMenu` (Android-only by design, 3.2).

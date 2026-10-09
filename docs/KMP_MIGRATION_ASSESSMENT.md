# Kotlin Multiplatform migration

Goal: move OnlineGo to Kotlin Multiplatform with **iOS as the second target** and **Compose
Multiplatform as the end state**, without a long-lived branch - **the Android app builds and ships
at the end of every phase.**

**Verdict: feasible.** The UI is 100% Compose (one XML layout, a notification `RemoteViews`), there
is no RxJava, no `Parcelable`, and Koin, Molecule, Room, DataStore, `ViewModel`, Turbine and
kotlinx-collections-immutable are already multiplatform. The cost is dominated by resources
(663 strings x 16 locales) and the local-AI engine, which cannot run on iOS as built today.

**Status.** Phase 0 is finished apart from one re-test: nineteen slices are done (section 6). None
has shipped in a release yet, and several still need on-device checks (section 1.2). Phases 1-5
have not started.

Earlier, longer versions of this document:
`git show d529259:docs/KMP_MIGRATION_ASSESSMENT.md` (original assessment) and
`git show acedf6a:docs/KMP_MIGRATION_ASSESSMENT.md` (full write-ups of slices 6.1-6.15).

---

## 1. What is left

### 1.1 Phase 0 - de-coupling (Android-only, no KMP tooling)

- [ ] **Re-test Material3's `ExposedDropdownMenu`.** If its `LazyColumn` performance problem is
  fixed, `ui/composables/ScrollableDropDownMenu.kt` (671 LOC, raw `PopupWindow`, cannot be
  ported) can be deleted instead of rewritten.
- [ ] *Optional tidy:* add `= null` to the 129 nullable-without-default DTO properties. Behaviour is
  already covered by the `Json` config.

**Owed after a release ships:** delete the legacy `CookiePersistence` prefs file, the
`SerializableCookie` shim and the import branch (6.5). They are the rollback path until then.

**Product call:** raising `minSdk` to 26 is the only way to drop core library desugaring (6.7).

### 1.2 On-device checks owed for finished work

Verified on device: the session survives an in-place upgrade, password login, Google sign-in, and
declining a challenge (an unsafe request, so CSRF works).

Still owed - unit tests cannot reach these:

- [ ] **The other seven body-bearing endpoints** (6.4): `createAccount`, `openChallenge`,
  `challengePlayer`, `markPuzzleSolved`, `ratePuzzle`, `acknowledgeWarning`, `deleteAccount`.
- [ ] **Crashlytics breadcrumbs** arrive as `I/Tag: message`, and release logcat is silent (6.6).
- [ ] **The Ktor websocket** (6.14): connect and authenticate, reconnect after airplane mode, an
  idle socket staying up past a minute, and logout sending `cleanup()` before the close frame.
- [ ] **Game connection ref-counting** (6.15): open, leave and reopen a game; a game in the active
  list keeps receiving moves after its screen closes; chat turns on when the screen opens.
- [ ] **Type-safe navigation on a release (R8) build** (6.19): every tab, a game, a tutorial, a
  puzzle, onboarding via login and sign-up, and the deep link
  `adb shell am start -a android.intent.action.VIEW -d "sente://game/76828314/9/9"`. Check the
  `screen_view` names in Firebase DebugView.
- [ ] **Clock text** (6.16): simple, byo-yomi, Canadian and Fischer clocks and the game-list
  timers, in English and in a locale with a decimal comma (e.g. `de`).

### 1.3 Phases 1-5

| Phase                                | Work                                                                                                                                                                                                                                                           | Estimate                        |
|--------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| **1 - extract `:shared`**            | One library module holding `model`, `logic`, `network`, `database`, `data`. `:app` keeps the Activity, notifications, WorkManager, billing, Play services and UI. Surfaces every dependency cycle before KMP can muddy the diagnosis.                          | a few days                      |
| **2 - make `:shared` multiplatform** | `androidTarget` + iOS targets; everything starts in `androidMain`. Move `model` + `logic` (~950 LOC) to `commonMain`, `RulesManagerTest` to `commonTest`. `RulesManager` still holds `androidx.core.util.lruCache` and the JNI estimator (4.1).                | 1-2 weeks (+1-2 for estimator)  |
| **3 - data layer to `commonMain`**   | Room KMP (bundled SQLite driver, `expect`/`actual` DB path), DataStore on okio `Path`, Ktor engine per platform, repositories one file at a time. Session persistence is behind `SessionCookiePersistence`; the websocket has no JVM threading left.           | 4-6 weeks                       |
| **4 - Compose Multiplatform**        | UI into `:shared/commonMain`. Resources (4.2), icons, chart, markdown, `ScrollableDropDownMenu`, Material You. `BoardComposable` has four Android leaks with direct CMP equivalents: `nativeCanvas.drawText`, `pointerInteropFilter`, `Rect`, `colorResource`. | 6-10 weeks (resources dominate) |
| **5 - iOS app**                      | SwiftUI shell + `ComposeUIViewController`, notifications, background refresh, billing, Google sign-in, App Store plumbing. Bitrise needs a macOS stack.                                                                                                        | 4-8 weeks, excluding local AI   |

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
iosApp/    Xcode project - SwiftUI shell, BGTaskScheduler, StoreKit, UNUserNotificationCenter
```

Layering inside `:shared` lives in package names; `commonMain` / `androidMain` / `iosMain` do the
job a module split would, since the compiler rejects `android.*` in `commonMain`.

A nine-module `:core:*` split is the default advice and wrong here: no `build-logic` (nine
hand-kept build files on a brand-new toolchain), one `Res` class per module fragments the
resources work, `model` + `logic` is 950 LOC, and nearly every commit has one author. Revisit when
clean-build time becomes a real problem (record a baseline first), a second regular contributor
appears, or local AI needs Play Feature Delivery for its 265 MB of assets.

---

## 3. Dependencies

**Done (section 6):** Moshi and `org.json`, jsoup, Coil 2, Retrofit, PersistentCookieJar,
`android.util.Log`, `java.time`, direct OkHttp use, Firebase Analytics and Crashlytics call sites,
`java.util.concurrent` / `UUID` / `Stack`, string navigation routes.

| Dependency                                      | Replacement                                                                                                    | When                |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------|---------------------|
| `BuildConfig`                                   | BuildKonfig                                                                                                    | Phase 2             |
| Koin Android artifacts                          | `koin-core` + `koin-compose` + `koin-compose-viewmodel`; drop `androidApplication()`                           | Phase 2             |
| Molecule `AndroidUiDispatcher` / `ContextClock` | multiplatform frame clock; extract the shared `moleculeScope` base from the two ViewModels                     | Phase 2             |
| Room 2.8, DataStore 1.2                         | same libraries, KMP setup                                                                                      | Phase 3             |
| MPAndroidChart                                  | Vico 2.x / KoalaPlot / Compose `Canvas`; confined to `ChartWrapper.kt` (416 LOC)                               | Phase 4             |
| Markwon                                         | `multiplatform-markdown-renderer-m3` (`JosekiExplorerUI`, ~90 LOC)                                             | Phase 4             |
| `material-icons-extended` (109 refs, 21 files)  | vendored `ImageVector`s                                                                                        | Phase 4             |
| navigation-compose (androidx)                   | `org.jetbrains.androidx.navigation`; routes are already `@Serializable` (6.19)                                 | Phase 4             |
| Android resources (`R.*`)                       | Compose Multiplatform resources - see 4.2                                                                      | Phase 4             |
| `AppLocaleManager`                              | `expect`/`actual`; iOS writes `AppleLanguages` and needs a restart                                             | Phase 4             |
| WorkManager                                     | `expect`/`actual` `BackgroundSync`; iOS `BGAppRefreshTask`                                                     | Phase 5 (see risks) |
| `NotificationUtils` (348 LOC)                   | `expect`/`actual`; re-rasterize the board with Compose, deleting `BoardView` (737 LOC) and the last XML layout | Phase 5             |
| Play Billing                                    | StoreKit, or RevenueCat `purchases-kmp`                                                                        | Phase 5             |
| Play In-App Review                              | `SKStoreReviewController`                                                                                      | Phase 5             |
| play-services-auth                              | Credential Manager / iOS Google SDK; the token exchange is already plain REST                                  | Phase 5             |
| Mockito, compose screenshot plugin              | stay on the Android target; new `commonTest` uses fakes + Turbine                                              | No change           |

**Stays Android-only by design:** `MainActivity` (edge-to-edge, deep links, `isInForeground`),
Material You dynamic color (`expect`/`actual`), the window theme in `styles.xml` (the only reason
the Material Components dependency exists), and `ScrollableDropDownMenu` unless Material3 makes it
redundant.

---

## 4. The two large workstreams

### 4.1 Local AI and the score estimator

These are separate problems.

**Score estimator (`libestimator.so`).** The JNI surface is one function, `RulesManager.estimate()`,
a clean `expect`/`actual` boundary. Options: cross-compile for iOS with `cinterop`; **port
`Goban.cpp` (~1,000 LOC) to Kotlin - recommended**, since it removes the NDK and all 1,840 LOC of
C++ and makes estimation testable in `commonTest` (benchmark first); or use OGS's server-side
estimate on iOS, at the cost of a round-trip on a latency-sensitive action.

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

663 strings (plus 10 `clock_*` entries, English-only until Crowdin catches up), 16 locale folders
(`ro` 658 translated down to 7 empty stubs), 456 `stringResource` sites in 39 files, 33 vectors,
3 board JPEGs, 1 `.ogg`. Compose Multiplatform resources read the same `strings.xml`, so Crowdin
needs only a path change. The seam already exists: `TextResource` and the `labelResId` /
`DetailValue.Resource` idiom carry ~130 resource ids as `Int` in ViewModel state, which become typed
`StringResource`s mechanically. The one non-mechanical part is
`ui/screens/game/TimeControlDescription.kt`, which calls `Resources.getQuantityString` outside
composition; the clock text (`ui/composables/ClockText.kt`) is already composable. Do it against
one module so there is one `Res` class.

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

## 6. Done - what is still load-bearing

Only traps, deliberate behaviour changes, and decisions that look wrong but are not. Unit tests
stand at 121.

### 6.1 Moshi to kotlinx.serialization

- One `appJson` (`utils/AppJson.kt`); each flag reproduces a Moshi behaviour. With
  `explicitNulls = false` an explicit `null` stays `null` rather than taking the default
  (`NullableDefaultsTest`).
- **`isLenient` does not cover numbers.** OGS sends `99999999.0` for an `Int?`.
  `LenientIntSerializer` / `LenientLongSerializer` accept exactly-integral floats and are applied
  per file through `@file:UseSerializers` - a new DTO file without it loses that leniency.
- `AnySerializer` reproduces Moshi's runtime types (`Double`, `LinkedHashMap`) because call sites
  cast to them. `OGSClock.receivedAt` and `AiGameState.chatText` are `@Transient`.
- The Room version was **deliberately left at 20**, so a stale `MoveTree` blob throws visibly
  instead of being wiped.

### 6.2 jsoup

`AnnotatedString.fromHtml()` at the render site in `TsumegoUI`; descriptions keep formatting and
links. It is Android-only, so it needs `expect`/`actual` or the markdown renderer in Phase 4.

### 6.3 Coil 2 to Coil 3

- Coil 3.6.3 forced compileSdk 37 and Compose 1.12 (3.4.0 would not have); `targetSdk` is 36.
- `rememberAsyncImagePainter` defaults to `Size.ORIGINAL`, so those sites use `AsyncImage`. The
  regression would be silent memory use.
- `processGravatarURL` rounds up to the CDN's 64/128/256/512 buckets (it always asked for 512).

### 6.4 Retrofit to Ktorfit

Traps, all found at runtime: `baseUrl` needs a trailing `/` and paths must not start with one;
`@Body` sets no `Content-Type` (`defaultRequest` fixes it); a `%` in a path crashes KSP, so the
Google OAuth constants are `@Query` defaults; both OkHttp and Ktor have `followRedirects = false`;
`expectSuccess` throws on 3xx, so the Google handshake opts out. Errors surface as
`OGSApiException`, with the body captured eagerly because catch sites are not `suspend`.

### 6.5 PersistentCookieJar to `OGSCookieStore`

- SharedPreferences, not DataStore, because `isLoggedIn()` is synchronous on cold start; hidden
  behind `SessionCookiePersistence`.
- **Only `csrftoken` and `sessionid`, only for the OGS host** - a trust boundary. If OGS needs a
  third cookie, login breaks with no obvious cause: look at `KEPT_COOKIES`.
- The legacy import decodes a Java-serialized blob: the shim must keep `serialVersionUID` and the
  simple class name `SerializableCookie`. The old file is the downgrade path; contraction is owed
  (1.1).

### 6.6 Logging to Kermit

`CrashlyticsBreadcrumbWriter` forwards Info+ as breadcrumbs and never records non-fatals
(`kermit-crashlytics` was rejected for that reason). Logcat is debug-only. Verbose/Debug use the
lambda overload, so release never builds those strings.

### 6.7 `java.time` to `kotlin.time` + kotlinx-datetime

- Stdlib `Instant` / `Clock`; kotlinx-datetime 0.8.0 for zones and formatting. Room still stores
  epoch millis. `OGSDateTimeTest` uses `java.time` as an oracle.
- **Desugaring stays**: kotlinx-datetime needs `java.time` below API 26.
- Fixed: the stats chart treated local time as UTC. Changed: the chart tooltip date uses
  `DateUtils`, because kotlinx-datetime has English month names only.

### 6.8 Constructor injection

`OnlineGoApplication.instance` is gone. The remaining `GlobalContext` sites are deliberate and do
not block KMP (Koin's global API is common code): lazy injection to break cycles in
`OGSWebSocketService` and `UserSessionRepository`, the two workers (no `WorkerFactory`),
`GameConnection`, and `Globals1.kt`'s `ClockDriftRepository`.

### 6.9 Firebase Analytics behind `Analytics`

`utils/Analytics.kt` is the only importer besides `Modules.kt`; parameters are a
`Map<String, String?>`. In Phase 2 it becomes a common interface with a Swift implementation
registered in Koin. Event names are unchanged.

### 6.10 Firebase Crashlytics behind `CrashReporter`

- A global `CrashReporter` object in `utils/CrashReporter.kt`, the only `FirebaseCrashlytics`
  importer; `expect object` in Phase 2. Global on purpose: `BoardComposable`'s draw code and
  `TextResource.resolve()` cannot take constructor parameters.
- **Behaviour change:** all reports go through the network-error filter, so cancellation and
  network errors are no longer reported and HTTP 5xx is wrapped in `ServerException`.

### 6.11 Layering

Nothing in `data.model` or `gamelogic` imports `R`, `android.graphics`, Compose UI or `ui`.
`BoardTheme`, `AppTheme`, `AppLanguage` and `TutorialIcon` carry no resource ids; their
presentation lives in exhaustive `when`s next to the UI (`BoardThemeStyle.kt`, `SettingsUI.kt`,
`LearnUI.kt`), so a new enum entry does not compile without its resources. Persisted forms are
unchanged. Still Android-bound in `data` (Phase 3 work): `Context` in five repositories,
`android.os.Build` in `HTTPConnectionFactory`.

### 6.12 `Globals.kt` split

`utils/Globals.kt` imports nothing Android-only. `timeControlDescription` moved to
`ui/screens/game/TimeControlDescription.kt`. `Pattern` became `Regex`, pinned by
`ProcessGravatarURLTest`. One JVM call is left: `System.currentTimeMillis()` in
`timeLeftForCurrentPlayer`.

### 6.13 JVM collections, atomics, `UUID` and `Locale`

- `kotlin.concurrent.atomics` is experimental in Kotlin 2.3; the opt-in is module-wide.
- `OGSWebSocketService.eventListeners` is copy-on-write over persistent collections; a listener
  removed mid-dispatch can still receive that one event.
- `TsumegoState.nodeStack` is an immutable `List`. `AppLanguage` compares language subtags as
  strings (`AppLanguageTest`).

### 6.14 Websocket from OkHttp to Ktor

- Shares the REST `HttpClient` with the `WebSockets` plugin; no cookies reach it (host-only, 6.5).
- **The plugin's `pingInterval` is ignored by the OkHttp engine.** The 15 s ping is on the shared
  `OkHttpClient`, so REST HTTP/2 connections are pinged too. Darwin sets it through the plugin.
- Outbound messages go through a per-connection `Channel(UNLIMITED)`; messages emitted during the
  handshake are queued. **`disconnect()` closes the queue rather than cancelling**, so `cleanup()`
  messages drain before the close frame.
- A bad incoming payload is reported and skipped instead of dropping the socket.
- **CIO was rejected** as the Android engine: HTTP/1.1 only, its own TLS stack, worse recovery on
  network switches, no DNS hook for the emulator, and OkHttp stays in the APK through Coil anyway.

### 6.15 JVM threading primitives in `data`

- `OGSWebSocketService` and `GameConnection` share a coroutine `Mutex`. Every former `runBlocking`
  waited on `loginStatus.first()`, which can really wait at cold start - do not swap it for
  `replayCache`.
- **Ref-counting lives in the service.** The old lock was re-entered by the same thread and a
  `Mutex` would deadlock there. `GameConnection.close()` is asynchronous: it launches `release()` on
  `applicationScope`.
- `ActiveGamesRepository` uses `AtomicReference`s to persistent collections;
  `FinishedGamesRepository`'s in-flight flag is an `AtomicBoolean`. `model/local/Clock.kt` spells
  out `kotlin.time.Clock` because its own class has the same name.

### 6.16 Clock text localized

- `computeTimeLeft` returns `PlayerClock(timeLeft, period)`, numbers only; `period` is a
  `ClockPeriod` (`Canadian`, `ByoYomi`, `Fischer`). `timeLeft == Long.MAX_VALUE` means no time
  limit and renders as `∞`.
- `clockFace(millis)` holds the rounding and bucketing (days / hours / minutes / tenths), pinned by
  `ClockFaceTest`. `ui/composables/ClockText.kt` formats it from the `clock_*` resources, with a
  plural for days. `String.format` is gone.
- `GameViewModel.TimerDetails` carries `PlayerClock`s and start-timer millis; the "to make first
  move" status is built in `GameUI` and still ranks below every other status.
- **Behaviour changes:** an expired clock shows `0.0s` instead of `0.0`, and decimals follow the
  locale (`1,5s` in German).

### 6.17 `RulesManager` thread checks

The three `Thread.currentThread()` checks only reported to Crashlytics and are deleted. What keeps
`RulesManager` out of `commonMain` now is `androidx.core.util.lruCache` and the JNI estimator, both
part of the estimator decision (4.1).

### 6.18 Dead weight

- Deleted: `viewBinding`, the `fileTree("libs")` dependency, `res/anim/`, both unused
  `*_PERIOD_MINUTES` constants (the `*_WORK_NAME` ones stay - they cancel work old installs
  scheduled), and `FacebookLoginCallbackActivity` with its manifest entry, its translucent theme and
  the runtime disable in `MainActivity`.
- `rang.hpp` was included through `log.h`, whose only use was a debug block in `Goban.cpp` that the
  JNI binding never enables (`debug = false`). The block and both headers are gone. This is the one
  edit to the upstream estimator code.
- `GamePresenterTest` became `utils/ClockFaceTest.kt` and `data/model/ogs/UserDecodingTest.kt`.

### 6.19 Type-safe navigation

- `ui/screens/main/Route.kt` is a `@Serializable sealed interface`. Each `@SerialName` is the old
  route string and each property name is the old argument key, so ViewModels still read
  `SavedStateHandle` by key, unchanged.
- **Route patterns are the Firebase `screen_view` names** (`analytics.logScreenView` logs
  `destination.route`) and the deep-link shape. Renaming a `@SerialName` or a route property changes
  both. Arguments without a default become path segments; those with a default become query
  parameters.
- Unchanged: every tab, `game/{gameId}/{gameWidth}/{gameHeight}` and its
  `sente://game/...` deep link (`navDeepLink<Route.Game>(basePath = "sente://game")`), `tutorial`,
  `tsumego`. Changed: `onboarding?initialPage={initialPage}` (was `{initialPageArg}`) and
  `otherPlayerStats/{playerId}`, now a `Long` path argument (nothing navigates there today).
- The bottom-bar tab list is the single source for both visibility and selection.
- R8 keeps the route serializers (checked in `mapping.txt`), but a release build has not run on a
  device yet (1.2).

# Kotlin Multiplatform migration

Goal: move OnlineGo to Kotlin Multiplatform with **iOS as the second target** and **Compose
Multiplatform as the end state**, without a long-lived branch - **the Android app builds and ships
at the end of every phase.**

**Verdict: feasible.** The UI is 100% Compose (one XML layout, a notification `RemoteViews`), there
is no RxJava, no `Parcelable`, and Koin, Molecule, Room, DataStore, `ViewModel`, Turbine and
kotlinx-collections-immutable are already multiplatform. The cost is dominated by resources
(663 strings x 16 locales) and the local-AI engine, which cannot run on iOS as built today.

**Status.** Phases 0-3 are finished: thirty-three slices are done (section 6). Phases 0-2 have
shipped in a release; Phase 3 has not, and several slices still need on-device checks (section 1.2).
Phase 3 moved the Room
database (6.25), the score estimator (6.26), `CrashReporter` (6.10), the HTTP client (6.27),
storage (6.28), the session cluster (6.29) and the remaining repositories (6.30) to common code,
wired by a shared Koin module (6.31) and an iOS platform module (6.32). Only the bindings Swift
supplies are left, and they come with the iOS app. The toolchain is set for Compose
Multiplatform 1.12.2 (6.33); phases 4-5 have not started.

Earlier, longer versions of this document:
`git show d529259:docs/KMP_MIGRATION_ASSESSMENT.md` (original assessment) and
`git show acedf6a:docs/KMP_MIGRATION_ASSESSMENT.md` (full write-ups of slices 6.1-6.15).

---

## 1. What is left

### 1.1 Phase 0 - de-coupling (Android-only, no KMP tooling)

Nothing left.

**Kept long-term:** the legacy `CookiePersistence` prefs file, the `SerializableCookie` shim and
the import branch (6.5). Users update late, and anyone coming straight from a pre-6.5 version is
logged out without them. Delete them only once the Play Console shows pre-6.5 installs are
negligible.

**Product call:** raising `minSdk` to 26 is the only way to drop core library desugaring (6.7).

### 1.2 On-device checks owed for finished work

Verified on device: the session survives an in-place upgrade, password login, Google sign-in,
declining a challenge (an unsafe request, so CSRF works), the score estimate after both JNI
renames (6.24, 6.26), Room in `commonMain` installed over the existing app (6.25), tutorials
opening with `tutorials.json` still in the app (6.22), storage in `commonMain` (6.28), the Koin
graph after 6.31, logout (6.29), tutorials and Joseki after 6.30, type-safe navigation on a
release (R8) build (6.19), and an idle websocket staying up past a minute after 6.32.

Still owed - unit tests cannot reach these:

- [ ] **The other seven body-bearing endpoints** (6.4): `createAccount`, `openChallenge`,
  `challengePlayer`, `markPuzzleSolved`, `ratePuzzle`, `acknowledgeWarning`, `deleteAccount`.
- [ ] **Crashlytics breadcrumbs** arrive as `I/Tag: message`, and release logcat is silent (6.6).
- [ ] **The Ktor websocket** (6.14): connect and authenticate, reconnect after airplane mode, and
  logout sending `cleanup()` before the close frame.
- [ ] **Game connection ref-counting** (6.15): open, leave and reopen a game; a game in the active
  list keeps receiving moves after its screen closes; chat turns on when the screen opens.
- [ ] **Tsumego puzzle picker** (6.20): it opens scrolled to the current puzzle, thumbnails render
  as rows scroll in, tapping a row selects it, and tapping outside, tapping the field again or
  pressing back closes it without reopening.
- [ ] **Clock text** (6.16): simple, byo-yomi, Canadian and Fischer clocks and the game-list
  timers, in English and in a locale with a decimal comma (e.g. `de`).

### 1.3 Phases 3-5

| Phase                              | Work                                                                                                                                                                                                                                                                                    | Estimate                        |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|
| **3 - data layer to `commonMain`** | Done (6.25, 6.27-6.32). Left for the iOS app: the Swift `Analytics`, `AppDataEraser` and `TutorialsSource` passed to `initKoin()`, and a real iOS `CrashReporter`.                                                                                                                      | done                            |
| **4 - Compose Multiplatform**      | UI into `:shared/commonMain`; Koin and Molecule multiplatform artifacts with it. Resources (4.2), icons, chart, markdown, Material You. `BoardComposable` has four Android leaks with direct CMP equivalents: `nativeCanvas.drawText`, `pointerInteropFilter`, `Rect`, `colorResource`. | 6-10 weeks (resources dominate) |
| **5 - iOS app**                    | SwiftUI shell + `ComposeUIViewController`, notifications, background refresh, billing, Google sign-in, App Store plumbing. Bitrise needs a macOS stack.                                                                                                                                 | 4-8 weeks, excluding local AI   |

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
DataStore (6.28).

| Dependency                                      | Replacement                                                                                                    | When                |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------|---------------------|
| Koin Android artifacts                          | `koin-core` + `koin-compose` + `koin-compose-viewmodel`; drop `androidApplication()`                           | Phase 4             |
| Molecule `AndroidUiDispatcher` / `ContextClock` | multiplatform frame clock; extract the shared `moleculeScope` base from the two ViewModels                     | Phase 4             |
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

| Risk                         | Notes                                                                                                                                                                                                             |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Toolchain currency**       | Kotlin 2.4.21 / AGP 9.3.3 / Gradle 9.7.1, inside the supported matrix (6.33). KMP plugin, Room KMP and Ktorfit work together for iOS with the configuration cache on; CMP is chosen (1.12.2) but not yet applied. |
| **Room on a live schema**    | v20, 4 auto-migrations, 13 converters, and `fallbackToDestructiveMigration` silently wipes caches on a bad migration. `MoveTree` blobs written by Moshi are now read by kotlinx (6.1).                            |
| **Localization regression**  | 663 strings x 16 locales moved wholesale; needs a mechanical diff, not eyeballing.                                                                                                                                |
| **iOS background behaviour** | `BGAppRefreshTask` has no interval guarantee, so turn notifications will be worse than Android's 15-minute poll without server push (APNs), which OGS would have to support. No FCM today.                        |
| **KataGo memory on iOS**     | A new class of risk that does not exist on Android, where the engine has its own process.                                                                                                                         |
| **Bandwidth**                | A multi-month refactor on a personal project. Phases 0-1 are the hedge: they pay off on their own.                                                                                                                |

---

## 6. Done - what is still load-bearing

Only traps, deliberate behaviour changes, and decisions that look wrong but are not. Unit tests
stand at 160: 99 in `:shared` host tests (84 of them in `commonTest`, which also run on the iOS
simulator), 14 in `:shared` `iosTest` (simulator only), 47 in `:app`.

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
  simple class name `SerializableCookie`. The old file is also the downgrade path; it stays until
  pre-6.5 installs are gone (1.1).

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

`OnlineGoApplication.instance` is gone. The remaining global lookups are deliberate: lazy
injection to break cycles in `OGSWebSocketService` and `UserSessionRepository`, the two workers
(no `WorkerFactory`), `GameConnection`, and `Globals1.kt`'s `ClockDriftRepository`.
**`GlobalContext` is JVM-only**; common code uses `KoinPlatform.getKoin()`, the same context on
Android (6.29).

### 6.9 Firebase Analytics behind `Analytics`

`Analytics` is a common interface (`commonMain` `utils/Analytics.kt`); `FirebaseAppAnalytics` in
`androidMain` is the only Firebase Analytics importer; `androidPlatformModule` binds it as
`single<Analytics>`. Parameters are a `Map<String, String?>`; event names are unchanged. iOS
registers a Swift implementation with the iOS Koin graph.

### 6.10 Firebase Crashlytics behind `CrashReporter`

- `expect object CrashReporter` in `commonMain` `utils/CrashReporter.kt`; the Android `actual`
  (`CrashReporter.android.kt`) is the only `FirebaseCrashlytics` importer. Global on purpose:
  `BoardComposable`'s draw code and `TextResource.resolve()` cannot take constructor parameters.
- **Behaviour change:** all reports go through the network-error filter, so cancellation and
  network errors are no longer reported and HTTP 5xx is wrapped in `ServerException`.
- The filter is the common `reportableException()`, pinned by `ReportableExceptionTest`; only
  `isPlatformNetworkError()` is per platform (`java.net` and the cancelled Google sign-in on
  Android, `AndroidNetworkErrorTest`). `OGSApiException` moved to `commonMain` with it.
- **The iOS `actual` is still a placeholder for reporting:** it logs reportable exceptions through
  Kermit and drops breadcrumbs, user id and keys. Its `isPlatformNetworkError()` covers the Darwin
  engine's and Ktor's errors since 6.32.

### 6.11 Layering

Nothing in `data.model` or `gamelogic` imports `R`, `android.graphics`, Compose UI or `ui`.
`BoardTheme`, `AppTheme`, `AppLanguage` and `TutorialIcon` carry no resource ids; their
presentation lives in exhaustive `when`s next to the UI (`BoardThemeStyle.kt`, `SettingsUI.kt`,
`LearnUI.kt`), so a new enum entry does not compile without its resources. Persisted forms are
unchanged. Still Android-bound in `data`: `android.os.Build` in `HTTPConnectionFactory`, which is
platform code by design.

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
  `OkHttpClient`, so REST HTTP/2 connections are pinged too. Darwin takes it from the plugin
  (`pingIntervalMillis` in `sharedModule`, 6.32).
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

The three `Thread.currentThread()` checks only reported to Crashlytics and are deleted.
`RulesManager` is in `commonMain` since 6.24.

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

### 6.20 `ScrollableDropDownMenu` on the standard `Popup`

- The puzzle picker (`TsumegoUI`) keeps its own menu because it must be lazy: each row calls
  `renderCollectionPuzzle` when it scrolls into view. Material3 1.4's `ExposedDropdownMenu` still
  lays out a scrolling `Column`, so it would render every puzzle in the collection on open.
- The ~370 LOC copy of Compose's Android popup window (`WindowManager`, `MotionEvent`, internal
  Compose resource ids, `UUID`) is replaced by `Popup` with the existing position provider. The
  file has no Android imports and is ready for `commonMain`.
- `PopupProperties(focusable = true)`: outside taps, including on the field, only close the menu.
  The old popup let them fall through to the screen underneath; letting them through would make a
  tap on the field close the menu and reopen it. The anchor is now `PrimaryNotEditable` (the field
  is read-only).

### 6.21 Nullable DTO properties default to `null`

Every nullable constructor property of a `@Serializable` class now declares `= null` (61 of them;
the earlier count of 129 predates later edits). Decoding and encoding are unchanged: with
`explicitNulls = false` an absent key was already `null` and nulls are still omitted. The flag stays
as the safety net for a new property added without a default, pinned in `NullableDefaultsTest` by a
test-local class.

### 6.22 `:shared` extracted

- A library at `shared/` holds `data/**`, `gamelogic/**`, `usecases/**` and the six `utils` files
  they need (`Globals`, `Globals1`, `AppJson`, `PersistenceManager`, `CrashReporter`,
  `Analytics`), plus the Room schemas. The estimator's C++ moved to `:app` in 6.23 and back in 6.26.
  Packages are unchanged, so JNI symbol names, serial names and the
  `-keep class io.zenandroid.onlinego.data.**` rule still match. No dependency cycle surfaced.
- **`:shared` applies the Compose compiler plugin** though it has no composables. Without it the
  model classes compile as external and unstable: six more unstable parameters in the app's
  compose report (`Challenge`, `Player`, `Tutorial`, ...). Strong skipping hides this from the
  skippable count, so compare `unstable` parameters in `app_release-composables.txt`, not
  skippable totals. `stability_config.conf`'s `data.*` matches the `data` package only.
- **Firebase Analytics and Crashlytics are dependencies of `:shared`** (`androidMain`) until
  Phase 3 does 6.9 and 6.10. `ApiException` arrives transitively.
- `BASE_URL` was a build field for a fixed value; it is now `const val OGS_BASE_URL` in
  `OGSHttpClient.kt`.
- `tutorials.json` stays in the app's assets: its keys are app string resources, and
  `TutorialsJsonParsingTest` stays with it.
- Kotlin does not smart-cast a property declared in another module; nine app sites now read the
  model property once instead of null-checking it and reading it again.
- CI: Bitrise runs the `:shared` tests in their own step (6.23); without it the 75 moved tests
  would silently stop running. The release APK matched the pre-move build apart from dex size
  (-116 bytes).

### 6.23 `:shared` on the KMP plugin

- `com.android.kotlin.multiplatform.library`, not `com.android.library` + KMP: that combination is
  deprecated in AGP 9 and removed in AGP 10. Targets: Android, `iosArm64`, `iosSimulatorArm64`.
- **The plugin has no `externalNativeBuild` and no `BuildConfig`.** `CMakeLists.txt` and the JNI
  glue live in `:app`; JNI symbols follow the Kotlin class, not the module, so
  `System.loadLibrary` in `:shared` finds the `.so` in the APK. `HTTPConnectionFactory` takes
  `logBodies`, which the app passes to `androidPlatformModule` from its `BuildConfig.DEBUG`.
- Source sets: `androidMain`, `androidHostTest`, `commonMain`, `commonTest`. Host tests also run
  `commonTest`. The task is `:shared:testAndroidHostTest`; Bitrise runs it as a `script` step,
  since `android-unit-test` looks for `testDebugUnitTest`.
- `kotlin.native.ignoreDisabledTargets=true`: the Linux Bitrise stack cannot build Apple targets
  and skips them.
- No core library desugaring in the library: the app's L8 desugars everything it packages, and host
  tests run on a JDK with `java.time`.
- The release APK's `.so` files and the compose stability report (246 composables, 6 unstable
  parameters, both from a full recompile) matched the pre-move build. Incremental builds write
  partial compose reports - compare only after `compileReleaseKotlin --rerun`.

### 6.24 Model and logic in `commonMain`

- `data/model/**`, `gamelogic/**`, the serializers (`AnySerializer`, `LenientNumberSerializers`,
  `OGSBooleanSerializer`, `OGSInstantSerializer`, `JsonValues`), `AppJson` and `Globals` compile
  for Android and iOS. Packages are unchanged.
- `GameConnection.kt`'s DTOs (`OGSClock`, `TimeControl`, `Players`, ...) are in
  `GameConnectionModels.kt`, same package; the connection class stays in `androidMain`.
  `currentClockMillis` moved to `Globals1.kt`, next to the drift-aware `computeTimeLeft` it calls.
- **`@Transient` without an import is `kotlin.jvm.Transient`**, which kotlinx.serialization ignores:
  `JosekiPosition.labels` was always decoded. Two no-op annotations were deleted rather than
  "fixed" to `kotlinx.serialization.Transient`, which would have changed decoding.
- **Renaming or moving the JNI class breaks scoring silently**: `UnsatisfiedLinkError` is caught
  and only logged. R8 keeps the name through the default native-method rule (checked with
  `dexdump`). The class is `NativeEstimator` since 6.26.
- `RulesManager`'s position cache is `androidx.collection.LruCache`. The old
  `androidx.core.util.lruCache` was an `android.util.LruCache` stub in host tests, so tests now
  exercise a real cache.
- Kotlin/Native rejects commas in backtick test names; six were reworded.
- `ChallengeParams` lost `@Keep`; `-keep class io.zenandroid.onlinego.data.**` covers it.

### 6.25 Room database in `commonMain`

- `Database`, `GameDao`, `PuzzleDao` and `DbTypeConverters` are in `commonMain`, same package.
  Version 20 and the schema are unchanged: `schemas/.../20.json` came out byte-identical, so the
  identity hash matches and an older APK still opens the file.
- **Android keeps the framework SQLite** (no `setDriver`); `androidPlatformModule` supplies the
  builder and `sharedModule` adds `fallbackToDestructiveMigration` and builds it. iOS gets
  `BundledSQLiteDriver` (`iosMain` only); its builder and path are in `iosPlatformModule` (6.32).
- Every DAO function is `suspend` or returns `Flow`, the `@Transaction` bodies included; native
  targets reject blocking DAOs. Callers were already in coroutines, so only `suspend` spread to
  private handlers in four repositories. DAO calls now hop to Room's executor.
- **`Database` in `Database.kt` resolves to the `androidx.room.Database` import**, not the class:
  the `expect object DatabaseConstructor` spells the class out in full. Otherwise KSP fails with
  "must implement a single interface of type RoomDatabaseConstructor".
- A suspend function reference does not fit `flatMap`; `insertAllGames` uses a lambda.
- `DbTypeConverters` is a plain class with member converters (no companion, no `@JvmStatic`).
  `@Suppress(RoomWarnings.QUERY_MISMATCH)` is honoured like `@SuppressWarnings` was.
- The Room Gradle plugin owns `schemas/`; KSP runs for Android and both iOS targets. `room-ktx`
  (empty since Room 2.7) and the unused `room` bundle are gone.
- `iosTest/.../DatabaseTest.kt` is the first DAO test: transactions, the `insertAllGames` merge
  and the `MoveTree` converter, on an in-memory bundled database. The Linux Bitrise stack skips it
  (6.23). The merge keeps only players, moves and undo: the incoming clock and phase win.

### 6.26 Score estimator on iOS

- The C++ is in `shared/src/nativeInterop/cpp`, one copy for both platforms. It matches upstream
  `online-go/score-estimator` master (`87c8f91`) apart from the 6.18 debug-block deletion; later
  upstream commits are a version bump and Python bindings.
- `estimator.cpp` is the C entry point, `estimator_estimate`, in place on the board array like
  upstream's `jsbindings.cc`. It owns the `EMSCRIPTEN` and `USE_THREADS` defines. Android's CMake
  builds it with `app/src/main/cpp/jnibindings.cpp`, now a thin JNI wrapper.
- `ScoreEstimator.determineTerritory()` is in `commonMain` and calls
  `internal expect object NativeEstimator`. The Android `actual` keeps `loadLibrary` and the
  `external fun`; **the JNI symbol is now `Java_..._gamelogic_NativeEstimator_estimate`**. The
  iOS `actual` pins the array and calls the cinterop binding.
- `CompileEstimator` in `shared/build.gradle.kts` runs `xcrun clang++` and `libtool` per iOS
  target (deployment target 15.0, Kotlin/Native's minimum); `estimator.def` embeds the
  `libestimator.a` in the cinterop klib and links `-lc++`. Only the iOS cinterop tasks depend on
  it, so the Linux stack never runs `xcrun`.
- `iosTest/.../ScoreEstimatorTest.kt` runs the real C++ on 9x9 and 19x19 on `Dispatchers.Default`,
  whose 512 KB iOS stacks are the tightest the estimator's stack-allocated grids will see. It
  cannot live in `commonTest`: host tests run on a JVM without the `.so`.

### 6.27 HTTP client in `commonMain`

- `OGSHttpClient` (`configureOGSClient`), `OGSRestAPI`, `Glicko2HistoryConverter` and
  `OGSCookieStore` are in `commonMain`, same package; Ktorfit and the Ktor serialization artifacts
  are `commonMain` dependencies. Ktorfit generates `_OGSRestAPIImpl` once, into
  `metadata/commonMain`, and every target compiles it.
- `OGSCookieStore` reads the clock through `kotlin.time.Clock` and uses
  `kotlin.concurrent.Volatile`.
- **Still in `androidMain`:** `HTTPConnectionFactory` (OkHttp, the emulator DNS hook, the 15 s
  ping), `SharedPrefsCookiePersistence` and `LegacyCookieImport`. The Darwin engine and its
  cases of `isPlatformNetworkError()` (6.10) came in 6.32.
- **The services waited for the session cluster** (done in 6.29). `OGSRestService` needs
  `UserSessionRepository`
  and `SettingsRepository`, `OGSWebSocketService` needs `UserSessionRepository` and
  `SocketDebugRepository`, and `GameConnection` needs `ChatRepository`. `UserSessionRepository` in
  turn needs `PersistenceManager` (SharedPreferences) and `ActivityManager`, so storage has to move
  first.
- Six tests moved to `commonTest` with `ktor-client-mock`, so Google sign-in, the CSRF header,
  cookie capture and the error mapping now also run on the iOS simulator. `OGSEngineBridgeTest`
  (OkHttp), `LegacyCookieImportTest` and `OGSDateTimeTest` (`java.time` oracle) stay host-only. Two
  test names lost a comma for Kotlin/Native.

### 6.28 Storage in `commonMain`

- `SettingsRepository` and `ReviewPromptRepository` are in `commonMain` and take a
  `DataStore<Preferences>`. The Android `preferencesDataStore` delegates stay in `androidMain`
  (`PreferenceDataStores.kt`), with the `SharedPreferencesMigration` from
  `io.zenandroid.onlinego_preferences`, so file names and migration are unchanged. iOS will build
  them with `PreferenceDataStoreFactory.createWithPath`, as `SettingsRepositoryTest` does on an
  okio temp file.
- **`PersistenceManager` stays synchronous and on the same SharedPreferences file.** It is in
  `commonMain` over a four-method `KeyValueStore`; `SharedPreferencesKeyValueStore` opens `login`,
  named in `androidPlatformModule`. A move to DataStore would have needed a migration that drops the
  old keys,
  taking the downgrade path and, if it failed, the session with it. `PersistenceManagerTest` pins
  the key names. The UIConfig and its timestamp are now two writes instead of one edit; a crash
  between them only forces a UIConfig refresh.
- `ReviewPromptRepository` lost its unused `UserSessionRepository` parameter, referenced only in a
  commented-out debug check.
- The iOS `KeyValueStore` and `SessionCookiePersistence` are on `NSUserDefaults` (6.32).

### 6.29 Session cluster in `commonMain`

- `UserSessionRepository`, `OGSRestService`, `OGSWebSocketService`, `GameConnection`,
  `ChatRepository`, `SocketDebugRepository` and `SocketConnectedRepository` moved together: they
  form one dependency cycle. `koin-core` is a `commonMain` dependency.
- **`UserSessionRepository` takes an `AppDataEraser` instead of a `Context`** (a lambda until
  6.31). The Android one calls `ActivityManager.clearApplicationUserData()`, which wipes everything
  and kills the process; iOS has no equivalent and must clear its own stores.
- `GlobalContext.get()` became `KoinPlatform.getKoin()` in the three lazy lookups (6.8).
- The `ebi` device id uses `Random.nextDouble()` instead of `Math.random()`; on the JVM the string
  has the same `0.xxx` shape it takes the digits from.

### 6.30 Remaining repositories in `commonMain`

- The other ten repositories, `GetUserStatsUseCase` and `Globals1` moved; `androidMain` now holds
  only `HTTPConnectionFactory`, the cookie persistence and legacy import, the DataStore delegates,
  `SharedPreferencesKeyValueStore`, `FirebaseAppAnalytics`, the Crashlytics `CrashReporter` and
  `NativeEstimator`.
- `retryOnIOException` catches `kotlinx.io.IOException`, a typealias of `java.io.IOException` on
  the JVM, so it retries the same OkHttp failures.
- **`TutorialsRepository` takes a `TutorialsSource`** (a lambda until 6.31); the app's one reads
  `tutorials.json` from its assets and the repository decodes it with `decodeFromString`. Phase 4
  can swap it for a Compose resource read.
- **Joseki label extraction** (`extractLabelsFromDescription`) is a top-level `internal` function
  on `Regex.replace` instead of `java.util.regex`'s `appendReplacement`, pinned by
  `JosekiLabelsTest`. A `$` or `\` label is now inserted literally instead of being read as a
  group reference.
- **Known bug, unchanged:** `<(.):([A-H]|[J-T]\d{1,2})>` groups as "one letter A-H, or J-T with
  digits", so marks in columns A-H (`<A:D4>`) never match and stay as raw text. The intended
  pattern is `[A-HJ-T]\d{1,2}`; fixing it changes what the Joseki explorer shows.

### 6.31 Koin modules follow the layers

- **`sharedModule`** (`commonMain` `di/SharedModule.kt`): `appJson`, the cookie store, the
  `HttpClient` (`HttpClient(engine)` plus `configureOGSClient` and `WebSockets`), Ktorfit, both
  services, the database and DAOs, every repository, `PersistenceManager` and the use case.
- **The platform contract** is what `sharedModule` gets but does not define: an `HttpClientEngine`,
  `SessionCookiePersistence`, `KeyValueStore`, the two `DataStore`s under `SETTINGS_DATA_STORE` and
  `REVIEW_PROMPT_DATA_STORE`, a `RoomDatabase.Builder<Database>`, `Analytics`, `AppDataEraser`,
  `TutorialsSource` and the application `CoroutineScope`. `iosTest/.../SharedModuleTest` starts
  Koin with stubs for exactly that list and resolves every definition, so a definition added
  without its platform binding fails there (checked by removing one).
- **`androidPlatformModule(logHttpBodies)`** (`androidMain`): OkHttp through
  `HTTPConnectionFactory`,
  the SharedPreferences stores, the DataStore delegates, the Room builder, Firebase Analytics and
  the `ActivityManager` eraser. It uses `get<Context>()`, so `:shared` needs no `koin-android`.
- **`:app`'s `Modules.kt` is the composition root**: the application scope, `TutorialsSource`
  (its asset), `AppLocaleManager`, notifications, `WhatsNewUtils`, KataGo, ViewModels, Play Store,
  Espresso and the review prompt. `:app` no longer depends on Ktor or Ktorfit directly.
- `AppDataEraser` and `TutorialsSource` are `fun interface`s because Koin cannot tell `() -> Unit`
  bindings apart; the lambdas of 6.29 and 6.30 only worked because `Modules.kt` built those
  repositories by hand.

### 6.32 iOS platform module

- `iosPlatformModule` (`iosMain` `di/IosPlatformModule.kt`) implements the 6.31 contract: the
  Darwin engine, the `NSUserDefaults` stores, and both DataStores and the database in Application
  Support (`settings.preferences_pb`, `review_prompt.preferences_pb`, `database.db`) on
  `BundledSQLiteDriver`.
- **`initKoin(analytics, appDataEraser, tutorialsSource)`** (`di/InitKoin.kt`) is the iOS
  composition root and adds the application scope. Swift implements the three Kotlin interfaces;
  until the iOS app exists, only `IosPlatformModuleTest` calls it.
- `KeyValueStore` is `UserDefaultsKeyValueStore` over the suite `login`, like Android's prefs file.
  **`getLong` reads `objectForKey`**: `integerForKey` returns 0 for a missing key, not the default.
- `SessionCookiePersistence` is `KeyValueCookiePersistence` over the suite `ogs_session`: the whole
  cookie map as one JSON string, so a save replaces the set in one write. Like SharedPreferences,
  `NSUserDefaults` is sandbox-private but unencrypted and backed up; Keychain would be the
  hardening.
- **Redirects:** NSURLSession follows redirects on its own, but Ktor's `KtorNSURLSessionDelegate`
  refuses them, so `followRedirects = false` holds and the Google handshake still sees the 3xx.
- **Websocket ping:** the Darwin session is wrapped by the `WebSockets` plugin, whose pinger is off
  by default. `sharedModule` sets `pingIntervalMillis = 15_000`; OkHttp ignores it in favour of its
  engine's 15 s (6.14).
- `isPlatformNetworkError()` on iOS: Ktor's socket and connect timeouts, and
  `DarwinHttpRequestException` with a connectivity `NSURLErrorDomain` code or, from the websocket
  path, a POSIX connection error (`IosNetworkErrorTest`).
- **Kotlin/Native's standalone simulator test run fails every TLS handshake** (`-1202` for any
  host), so real-network checks need a booted simulator: an init script that sets `device` and
  `standalone = false` on `iosSimulatorArm64Test`. Checked that way with a throwaway test: a REST
  GET decodes, a 301 surfaces as `OGSApiException`, and the websocket answers `net/ping` after 70 s
  idle. The shared `HttpClient` has no base URL - Ktorfit supplies it - so raw calls need absolute
  URLs.
- An iOS `AppDataEraser` must clear the database, both DataStores and both defaults suites itself
  (6.29).

### 6.33 Toolchain for Compose Multiplatform

- **Kotlin 2.4.21** (was 2.3.20). Kotlin 2.3.20-2.3.21 supports AGP only up to 9.0.0, so AGP 9.2.1
  was outside the supported matrix; 2.4.20-2.4.21 covers AGP 8.5.2-9.3.1 and Gradle up to 9.7.0.
  It is tested against Xcode 26.4; the local Xcode 27.0 builds and runs the simulator tests.
- **AGP 9.3.3, Gradle 9.7.1** (were 9.2.1 and 9.4.1): the newest of each inside the 2.4.21 range
  (patch releases past the listed 9.3.1 and 9.7.0). **AGP 9.3 refuses Gradle below 9.5**, and
  **AGP 9.4 is outside the 2.4.21 range.** AGP 9.3 warns that six `android.*` options in
  `gradle.properties` are deprecated; they go before AGP 10.
- **KSP 2.3.12** (was 2.3.6). KSP2 versions are no longer tied to the Kotlin version. Ktorfit 2.7.5
  infers its compiler plugin from the Kotlin version and needed no change.
- **Compose BOM 2026.09.00** (Compose 1.12.1, was 1.12.0): the version CMP 1.12.2 is built on, so
  Android keeps one Compose version when CMP arrives.
- **Target: Compose Multiplatform 1.12.2**, the latest stable. 1.13 is alpha and raises
  `compileSdk` to 37.1 and `minSdk` to 24. CMP's runtime 1.12.2 needs only stdlib 2.2.20, and its
  Android artifacts resolve to androidx Compose 1.12.1.
- **Material3 is the trap:** CMP 1.12 ships `org.jetbrains.compose.material3` 1.12.0-alpha03, based
  on Jetpack 1.5.0-alpha22. The stable choice is 1.9.0, which maps to Jetpack material3 1.4.0 -
  what the app ships today.
- The other CMP 1.12.2 libraries: Lifecycle 2.11.0, `org.jetbrains.androidx.navigation` 2.10.0-rc01
  (latest stable 2.9.2; the app uses androidx 2.9.7), resources 1.12.2. Koin 4.2.2's
  `koin-compose-viewmodel` needs Lifecycle 2.9.6+.
  `org.jetbrains.compose.material:material-icons-extended`
  stops at 1.7.3, so vendored `ImageVector`s (section 3) stand.
- Libraries that pair with Kotlin 2.4 and CMP 1.12.2: kotlinx.serialization 1.11.0, Ktor 3.5.2,
  Koin 4.2.2, Room 2.8.5, androidx Lifecycle 2.11.0, coroutines-test 1.11.0 (core already resolved
  to 1.11.0). **sqlite-bundled stays 2.6.2**, the version Room 2.8.5 declares. Left for their own
  slices: navigation (with the switch to the multiplatform artifact), kotlinx-collections-immutable
  0.5, and the Android-only Firebase BOM 35, OkHttp 5.5, core-ktx 1.19 and WorkManager 2.12.
- Checked: the release compose report is byte-identical (241 composables, 6 unstable parameters),
  R8 keeps the serializers of all five route classes (the `data object` routes have none), and all
  tests pass on Android and the iOS simulator. On a booted simulator, a Ktorfit call decodes, a 301
  surfaces as `OGSApiException`, and the websocket answers `net/ping` after 70 s idle.

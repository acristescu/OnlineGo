# Kotlin Multiplatform migration - feasibility assessment

Scope of this document: what it would take to move OnlineGo to Kotlin Multiplatform with **iOS as
the second target**, and **Compose Multiplatform as the intended end state**. It identifies the
blockers, names a concrete replacement for every dependency that is not KMP-ready, and sequences
the work so that every phase still ships on Android.

This is an assessment, not a commitment. Nothing here has been implemented.

Measured against the repository at commit `3f1d980`.

---

## 1. Executive summary

**Verdict: feasible.** The codebase is in unusually good shape for this - better than the average
Android app of its age.

What is already in our favour:

- **The UI is 100% Compose.** Zero Fragments, zero `viewBinding` usages, one XML layout in the whole
  project (and it is a notification `RemoteViews`, not a screen).
- **No RxJava.** Coroutines and `Flow` throughout; the Rx migration already happened.
- **Several core dependencies are already multiplatform**: Koin, Molecule,
  `kotlinx-collections-immutable`, Room (2.7+), DataStore (1.1+), `androidx.lifecycle.ViewModel`
  (2.8+), Turbine, `kotlinx-coroutines-test`.
- **The Go rules engine is 798 LOC in two files** and needs roughly fifteen lines changed to compile
  in `commonMain`.
- **No `Parcelable`/`@Parcelize` anywhere.** Nothing to port there.
- 57 of 202 source files (28%) already have zero `android.*`/`androidx.*` imports.

What the effort is actually dominated by, in order of cost:

1. **Resource migration** - 663 strings across 16 locale folders, 456 `stringResource` call sites,
   plus ~130 string/drawable resource ids stored as `Int` inside ViewModel state classes.
2. **Moshi to kotlinx.serialization** - Moshi is reflection-only here (`KotlinJsonAdapterFactory`),
   which has no multiplatform equivalent. ~55 model files, 6 custom adapters, 6 separate
   `Moshi.Builder()` instances.
3. **Firebase de-coupling** - ~95 `FirebaseCrashlytics` call sites across 32 files. This is the
   single most pervasive Android coupling in the project, deeper than `Context` itself, and most of
   it is `.log()` being used as a general-purpose logger.
4. **The local AI engine** - KataGo is launched as a **child process**, which iOS forbids outright.
   That architecture, not the compute backend, is the blocker: we already build against Eigen/CPU,
   which ports to iOS cleanly. See section 5.

Two phases of this plan (0 and 1) are worth doing **even if the KMP migration never happens** -
they are straightforward architectural improvements to the Android app.

---

## 2. Baseline measurements

| Metric                                   | Value                                                                            |
|------------------------------------------|----------------------------------------------------------------------------------|
| Gradle modules                           | 1 (`:app`)                                                                       |
| Kotlin LOC (`app/src/main/java`)         | 33,839                                                                           |
| ... of which `ui/`                       | 23,448 (69%)                                                                     |
| ... of which `data/`                     | 6,755                                                                            |
| Java files                               | 2 (`StoneType.java`, `AndroidLoggingHandler.java`)                               |
| Files with zero android/androidx imports | 57 / 202                                                                         |
| XML layouts                              | 1 (`res/layout/notification_board.xml`)                                          |
| Activities / Fragments                   | 2 / 0                                                                            |
| `@Composable` functions                  | 264 across 55 files                                                              |
| Strings / locale folders                 | 663 / 16 (Crowdin-managed)                                                       |
| Drawables                                | 46 files, 33 of them vectors                                                     |
| Unit tests                               | 37 `@Test` in 8 files; 8 Compose screenshot previews; no real instrumented tests |

---

## 3. Target architecture - and how many Gradle modules we actually need

### The floor is two modules, not nine

KMP needs **one** non-application module to carry the iOS targets and produce the framework.
(`com.android.application` plus `kotlin("multiplatform")` in a single module is technically
possible but off the beaten path; AGP 9's `com.android.kotlin.multiplatform.library` is the
supported shape.) A second boundary earns its keep by stopping the shared code from reaching for
`R`, the Activity, or app-level Android APIs.

Everything beyond that is an architectural preference, not a KMP requirement. It is worth being
explicit about which is which, because a fine-grained `:core:*` split is the default advice in most
KMP write-ups and it is the wrong default for this project.

### Recommended: three

```
:shared    KMP library - model, logic, network, database, data, and (from phase 4) UI
:app       Android application - MainActivity, notifications, WorkManager,
           Play Billing, Play Review, Google sign-in
iosApp/    Xcode project - SwiftUI shell + ComposeUIViewController,
           BGTaskScheduler, StoreKit, UNUserNotificationCenter
```

Inside `:shared`, the layering survives as **package names** - `model`, `logic`, `network`,
`database`, `data`, `designsystem`, `ui` - so the intended structure stays legible without seven
extra build files.

### Why: source sets already do the job modules were being asked to do

The entire motivation for incremental module extraction is keeping Android green while migrating
piece by piece. `commonMain` / `androidMain` / `iosMain` inside a single `:shared` module delivers
exactly that, more cheaply, and with a **stronger** guarantee: the compiler rejects an `android.*`
import in `commonMain`. A module boundary is a coarser, more expensive version of the same check.

Migration then reads as "move this file from `androidMain` to `commonMain`" - one file at a time,
always compiling, no Gradle churn.

### What a nine-module split would actually cost us

- **No convention-plugin infrastructure exists.** There is no `buildSrc` and no `build-logic` -
  just one `build.gradle.kts` and a version catalog. Nine KMP modules means nine hand-maintained
  copies of target, source-set and compiler-plugin configuration, or building convention plugins
  first. That is a project before the project.
- **Compose Multiplatform resources are per-module.** Each module generates its own `Res` class in
  its own package. Splitting 663 strings across `:core:ui` and feature modules means several `Res`
  objects and cross-module resource friction. One module means one `Res` and one Crowdin path -
  and resources are already the single largest workstream (section 4.4).
- **Per-module plugin setup multiplies**: KSP for Room, the Compose compiler with our
  `stability_config.conf`, the screenshot plugin.
- **Nine modules against Kotlin 2.3.20 / AGP 9.2.1** is nine places for toolchain friction, on the
  exact risk ranked first in section 7.
- `model` and `logic` together are 950 LOC. That is a package, not a module.

### Arguments for more modules, and why they do not apply here

| Argument                        | Assessment                                                                                                                                                                                                                            |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build speed                     | 33.8k LOC in one module compiles fine, and a deep graph can serialize and get *slower*. Measure before believing it - record today's clean-build time as a baseline.                                                                  |
| Parallel work / merge conflicts | 197 of the last 199 commits (18 months) are from one author. Not a factor.                                                                                                                                                            |
| Reuse                           | One app, no second consumer.                                                                                                                                                                                                          |
| Enforcing layering              | A real benefit - but Phase 0 fixes the actual violations directly (`RulesManager` importing `ui.screens.game.Variation`; `UserStats` depending on MPAndroidChart). Adding a boundary later, once the code is already clean, is cheap. |

### When to revisit

Concrete triggers, not vibes:

- Clean build time crosses a genuinely annoying threshold, measured against the recorded baseline.
- A second regular contributor appears.
- The local-AI feature gets Play Feature Delivery for its 265 MB of assets. This is the one real
  module candidate in the project - and note it is an Android *packaging* concern, not a KMP one.

Governing rule for all of it: **the Android app must build and ship at the end of every phase.** No
long-lived migration branch.

---

## 4. Dependency-by-dependency assessment

### 4.1 Already multiplatform - keep, swap artifact only

| Dependency                       | Current                                         | Action                                                                                                                                                                                                                                                                                                                                                |
|----------------------------------|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Koin                             | 4.2.0, `koin-android` + `koin-androidx-compose` | Swap to `koin-core` + `koin-compose` + `koin-compose-viewmodel`. Remove `androidApplication()` from `di/Modules.kt`, and the five `GlobalContext.get()` service-locator sites (`OGSWebSocketService`, `GameConnection`, `UserSessionRepository`, `CheckNotificationsTask`, `SynchronizeGamesWork`, `Globals1.kt`) that exist only to break DI cycles. |
| Molecule                         | 2.2.0                                           | Already KMP. Only `AndroidUiDispatcher` / `RecompositionMode.ContextClock` need swapping for the multiplatform frame clock. Affects exactly two ViewModels (`GameViewModel`, `FaceToFaceViewModel`), which duplicate the same `moleculeScope` boilerplate - extract a base class while doing it.                                                      |
| androidx.lifecycle `ViewModel`   | 2.10.0                                          | KMP since 2.8. `SavedStateHandle` (5 ViewModels) is covered by `androidx.savedstate` 1.3.                                                                                                                                                                                                                                                             |
| Room                             | 2.8.4                                           | **KMP since 2.7.** Keep it. Needs the `androidx.sqlite` bundled driver and an `expect`/`actual` for the database file path. The v20 schema with 4 auto-migrations is the risk here, not the API.                                                                                                                                                      |
| DataStore Preferences            | 1.2.1                                           | **KMP since 1.1.** Keep. Move the three stores (`SettingsRepository`, `ReviewPromptRepository`, `WhatsNewUtils`) off `Context.preferencesDataStore` onto the okio `Path`-based factory.                                                                                                                                                               |
| kotlinx-collections-immutable    | 0.4.0                                           | No change.                                                                                                                                                                                                                                                                                                                                            |
| Turbine, kotlinx-coroutines-test | 1.2.1 / 1.10.2                                  | No change.                                                                                                                                                                                                                                                                                                                                            |
| `@Immutable` / `@Stable`         | compose-runtime                                 | No change - the Compose runtime is multiplatform. These annotations on `Position`, `Cell`, `StoneType` etc. are fine in `commonMain`.                                                                                                                                                                                                                 |

### 4.2 Must be replaced

| Current                                                                                  | Why it fails                                                                                                                                 | Recommended replacement                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Retrofit 3.0.0**                                                                       | JVM/Android only                                                                                                                             | **Ktorfit** over **Ktor 3**. Ktorfit is KSP-based and keeps the annotated-interface style, so `OGSRestAPI.kt` (33 endpoints, all already `suspend`) migrates close to 1:1. Engines: `OkHttp` on Android, `Darwin` on iOS. `CustomConverterFactory` (the hand-rolled TSV parser for the Glicko2 rating history) becomes a Ktorfit converter.                                                                                                                                                                                                                                                        |
| **OkHttp 5.3.2** (direct use)                                                            | 5.x has KMP artifacts but constrained; our usage is deeply Android-flavoured                                                                 | Keep as the *Android engine* under Ktor. `HTTPConnectionFactory` needs rewriting as Ktor plugins: the referer/CSRF/JWT network interceptor, `followRedirects(false)`, and the `EmulatorDnsSelector` (which is ~18 lines of `android.os.Build` fingerprint checks and becomes Android-only config).                                                                                                                                                                                                                                                                                                 |
| **Moshi 1.15.2, reflective**                                                             | `KotlinJsonAdapterFactory` needs `kotlin-reflect`; no multiplatform equivalent                                                               | **kotlinx.serialization**. Mechanical but wide: ~55 DTO files, but only 7 `@Json(name=)` annotations to convert to `@SerialName` (everything else relies on field-name matching, which is in our favour). The 6 custom adapters (`OGSInstantJsonAdapter`, `OGSBooleanJsonAdapter`, `HashMapOfCellToStoneTypeMoshiAdapter`, `AiDifficultyMoshiAdapter`, `ResponseBriefMoshiAdapter`) become `KSerializer`s; the `PolymorphicJsonAdapterFactory` for `TutorialStep` becomes a sealed hierarchy with `@SerialName`. Consolidate the 6 scattered `Moshi.Builder()` instances into one injected `Json`. |
| **`org.json`** in `OGSWebSocketService.kt`                                               | Android-only (AOSP-bundled)                                                                                                                  | kotlinx.serialization `JsonArray` / `JsonElement`. The OGS wire format is `[event, data]` / `[id, data, error]` - trivially expressible.                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| **PersistentCookieJar** (`com.github.franmontiel`)                                       | backed by Android SharedPreferences                                                                                                          | Ktor `HttpCookies` plugin with a custom `CookiesStorage` persisted through DataStore. Affects `UserSessionRepository` (`sessionid` detection, CSRF token extraction).                                                                                                                                                                                                                                                                                                                                                                                                                              |
| **Coil 2.7.0**                                                                           | 2.x is Android-only                                                                                                                          | **Coil 3.x** + `coil-network-ktor3`. Six files, mechanical. Note that 7 of the 13 `LocalContext.current` uses in the whole app exist only to build a Coil 2 `ImageRequest` - they disappear for free.                                                                                                                                                                                                                                                                                                                                                                                              |
| **MPAndroidChart v3.1.0**                                                                | Android-only, **and it has leaked out of the UI layer** into `data/model/local/UserStats.kt` and `usecases/GetUserStatsUseCase.kt`           | **Vico 2.x** (has KMP targets) or **KoalaPlot**, or a hand-rolled Compose `Canvas`. Fix the layering violation first - `UserStats` is a domain model and must not depend on a charting library. `ui/screens/stats/ChartWrapper.kt` is a 416-LOC rewrite.                                                                                                                                                                                                                                                                                                                                           |
| **Markwon 4.6.2**                                                                        | renders into an `android.widget.TextView` via `AndroidView`                                                                                  | **`com.mikepenz:multiplatform-markdown-renderer-m3`**. Contained to one screen (`JosekiExplorerUI.kt`, ~90 LOC).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **jsoup 1.22.1**                                                                         | pure JVM - survives Android and Desktop, breaks iOS                                                                                          | **Ksoup** (`com.fleeksoft.ksoup`), or simply delete it: the only usage is a 3-line `parseHtml()` in `TsumegoViewModel` that strips tags from puzzle descriptions.                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| **`material-icons-extended`**                                                            | deprecated Android artifact; 109 references in 21 files                                                                                      | Vendor the icons actually used as `ImageVector` declarations (best for binary size), or use `br.com.devsrsouza:compose-icons`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| **navigation-compose 2.9.7** (androidx)                                                  | Android artifact                                                                                                                             | `org.jetbrains.androidx.navigation:navigation-compose`. Take the opportunity to convert the 13 string-literal routes in `Navigation.kt` to `@Serializable` type-safe routes - we will already have kotlinx.serialization on the classpath, and the current bottom-bar logic matches routes against a hardcoded `listOf("myGames", "learn", "stats", "settings")`.                                                                                                                                                                                                                                  |
| **Firebase Crashlytics / Analytics**                                                     | Google Android SDK                                                                                                                           | Introduce our own `Logger`, `CrashReporter` and `Analytics` interfaces and inject them. Android actual delegates to Firebase; iOS actual to the Firebase iOS SDK (CocoaPods) or GitLive's `firebase-kotlin-sdk`. The bulk of the ~95 call sites are `FirebaseCrashlytics.getInstance().log(...)` used as a logger - route those to **Kermit**, which ships a Crashlytics log writer. A thin wrapper already exists (`utils/Crashlytics.kt`, used in 34 files); the problem is that most files bypass it.                                                                                           |
| **`android.util.Log`** (68 sites, 26 files)                                              | Android-only                                                                                                                                 | **Kermit** (`co.touchlab:kermit`). Same change as above; do them together.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| **`java.time`** (16 files)                                                               | JVM; currently works on `minSdk 23` only via core library desugaring                                                                         | **kotlinx-datetime**. Lets us drop `coreLibraryDesugaring` and `desugar_jdk_libs` entirely.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **`java.text.SimpleDateFormat`** (3 files)                                               | JVM                                                                                                                                          | kotlinx-datetime `DateTimeFormat`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **`java.util.concurrent`** - `ConcurrentHashMap`, `AtomicInteger/Boolean/Long` (5 files) | JVM                                                                                                                                          | `kotlin.concurrent.Atomic*` from the stdlib; `ConcurrentHashMap` in `OGSWebSocketService` becomes a plain map guarded by a `Mutex`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **`java.util.UUID` / `Stack` / `LinkedList` / `Locale`**                                 | JVM                                                                                                                                          | `kotlin.uuid.Uuid`, `ArrayDeque`, `expect`/`actual` for locale.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| **`BuildConfig`** (`BASE_URL`, `DEBUG`)                                                  | AGP-generated                                                                                                                                | **BuildKonfig**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **WorkManager 2.11.2**                                                                   | Android-only. One `CoroutineWorker` (`SynchronizeGamesWork`, 15-min periodic poll) delegating to `CheckNotificationsTask`                    | `expect`/`actual` `BackgroundSync`. Android keeps WorkManager; iOS gets `BGAppRefreshTask`, which is **best-effort with no interval guarantee**. Flag this honestly: the correct answer for iOS is server push (APNs), which OGS would have to support. The app currently has no FCM at all - background updates are poll-only.                                                                                                                                                                                                                                                                    |
| **`NotificationUtils.kt`** (348 LOC)                                                     | `NotificationManager`, `PendingIntent`, `RemoteViews`, and it **rasterizes a board by instantiating the legacy `BoardView`** into a `Bitmap` | `expect`/`actual` notification surface; iOS `UNUserNotificationCenter`. Re-implement the board rasterization on top of the Compose renderer (`ImageBitmap` + `Canvas`) - that deletes `ui/views/BoardView.kt` (737 LOC) and `res/layout/notification_board.xml`, the last legacy View and the last XML layout in the project.                                                                                                                                                                                                                                                                      |
| **Play Billing 9.1.0**                                                                   | Android-only                                                                                                                                 | `expect`/`actual`; iOS StoreKit. Consider **RevenueCat's KMP SDK** / `purchases-kmp` to avoid writing two store integrations. Contained to `playstore/PlayStoreService.kt` + `SupporterViewModel`.                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Play In-App Review 2.0.2**                                                             | Android-only                                                                                                                                 | `expect`/`actual`; iOS `SKStoreReviewController`. Contained to `utils/ReviewPromptManager.kt` + `ReviewPromptRepository`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **play-services-auth 21.5.1**                                                            | Android-only                                                                                                                                 | Credential Manager on Android; `GoogleSignIn` SDK or `ASWebAuthenticationSession` on iOS. Low risk - the token exchange itself is already plain REST against OGS.                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| **`gms.common.util.IOUtils`** in `HTTPConnectionFactory`                                 | gratuitous Play Services dependency in the networking layer                                                                                  | Delete. It is used only for gzip detection inside a log statement.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **`AppLocaleManager`**                                                                   | `LocaleManager`, `LocaleList`, `Resources.getSystem()`, `attachBaseContext` wrapping, synchronous SharedPreferences                          | `expect`/`actual`. iOS sets `AppleLanguages` in `UserDefaults` and requires a restart.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **Android string / drawable resources**                                                  | `R.*`                                                                                                                                        | **Compose Multiplatform resources** (`composeResources/`). It consumes the *same* `strings.xml` format, so Crowdin only needs a path change in `crowdin.yml`. Preferred over moko-resources, which is effectively in maintenance. See 4.4.                                                                                                                                                                                                                                                                                                                                                         |
| **Mockito** (8 test files, 37 tests)                                                     | JVM-only                                                                                                                                     | Leave those tests in `androidUnitTest`. New `commonTest` coverage uses hand-written fakes plus Turbine and `kotlinx-coroutines-test`, both of which are already multiplatform.                                                                                                                                                                                                                                                                                                                                                                                                                     |
| **`com.android.compose.screenshot`**                                                     | AGP-only tooling                                                                                                                             | Keep it on the Android target. Roborazzi is the option if we later want screenshot tests shared across JVM targets.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |

### 4.3 Stays platform-specific by design

- `MainActivity` - `enableEdgeToEdge`, `SystemBarStyle`, deep links, the `isInForeground` flag that
  the worker reads.
- **Material You dynamic color** - `dynamicLightColorScheme(context)` /
  `dynamicDarkColorScheme(context)`
  in `ui/theme/Theme.kt` are Android-only; wrap in `expect`/`actual` returning a `ColorScheme`.
- `res/values/styles.xml` - the two window themes (`AppTheme`, `Theme.OnlineGo.Translucent`). These
  exist purely for window background and translucency; `com.google.android.material:material` is a
  dependency solely to supply `Theme.Material3.*`. Real theming is already Compose-native.
- **`ui/composables/ScrollableDropDownMenu.kt` (671 LOC)** - a fork of Material3's
  `ExposedDropdownMenu` (swapping `Column` for `LazyColumn` because the stock one performs badly).
  It builds a raw `android.widget.PopupWindow` and reaches into `findViewTreeLifecycleOwner` /
  `setViewTreeSavedStateRegistryOwner`. **It cannot move to common code at all.** Before rewriting
  it, re-test current Material3 - the performance problem it works around may be fixed, in which
  case this file just gets deleted.

### 4.4 Resources - the largest single workstream

The numbers: 663 strings, 16 locale folders (`ro` 658 translated, `ca`/`es`/`de` ~640, `fr` 479,
`zh` 166, `pl` 73, `ru` 47, `sk` 20, and 7 empty stubs), 456 `stringResource` call sites across 39
files, 33 vector drawables, 3 board-texture JPEGs, 1 `.ogg` sound.

Two things make this tractable:

1. Compose Multiplatform resources use the same `strings.xml` format under
   `composeResources/values/`. Crowdin needs only a path update.
2. **The codebase already has the right seam.** `ui/composables/TextResource.kt` and the
   `labelResId` / `DetailValue.Resource` idiom mean roughly 130 resource ids are already funnelled
   through a wrapper rather than resolved inline. `TextResource(R.string.x)` becomes
   `TextResource(Res.string.x)`; the `Int` field becomes a typed `StringResource`. ViewModels that
   currently hold `Int` res-ids in state (`GameViewModel` ~30, `OnboardingViewModel` ~25,
   `AiGameViewModel` ~20, `MyGamesViewModel` ~18, `FaceToFaceViewModel` ~15) convert mechanically.

The non-mechanical parts: `utils/Globals.kt` passes an `android.content.res.Resources` into
`timeControlDescription()` and `formatSeconds()` and calls `getQuantityString` directly - that file
(410 LOC) needs splitting into pure time/rank arithmetic and a separate formatting layer.
`NotificationUtils` has ~20 more resource lookups, but that file is staying Android-only anyway.

---

## 5. The native code - two separate problems

These are routinely conflated. They are not the same problem and they do not have the same answer.

### 5.1 Score estimator (`libestimator.so`)

`app/CMakeLists.txt` builds one shared library from `app/src/main/cpp/` (2,544 LOC, of which only
~1,030 is real logic in `Goban.cpp`; `rang.hpp` is a 528-line terminal-colour header that is dead
weight on Android). It is a port of OGS's Monte-Carlo territory estimator.

**The JNI surface is exactly one function:**

```kotlin
// gamelogic/RulesManager.kt:37
private external fun estimate(
  w: Int, h: Int, board: IntArray, playerToMove: Int,
  trials: Int, tolerance: Float
): IntArray
```

That is a textbook `expect`/`actual` boundary. Three options:

| Option                                         | Effort     | Notes                                                                                                                                                                                                                                                                                  |
|------------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| (a) Cross-compile for iOS + `cinterop`         | Low-medium | Well-bounded C++11, no platform APIs. Needs a static-lib build for `ios-arm64` and the simulator, plus a `.def` file. Keeps the NDK in the Android build.                                                                                                                              |
| **(b) Port `Goban.cpp` to Kotlin**             | ~1-2 weeks | **Recommended.** Removes the NDK from the build entirely (faster CI, no ABI matrix), makes score estimation testable in `commonTest`, and deletes ~1,500 LOC of dead C++. Playout-based Monte Carlo is a good fit for Kotlin/Native performance-wise, but benchmark before committing. |
| (c) Android-only `actual`, iOS uses the server | Lowest     | OGS exposes server-side estimation. Adds a network round-trip to a latency-sensitive interaction.                                                                                                                                                                                      |

### 5.2 KataGo local AI - hard blocker on iOS

This is the one genuinely unresolved item, and it deserves a spike of its own.

**The problem is not JNI - it is that KataGo is run as a child process.**
`ai/KataGoAnalysisEngine.kt` does:

```kotlin
ProcessBuilder("./libkatago.so", "analysis", "-model", ..., "-config", ...)
.directory(File(applicationInfo.nativeLibraryDir))
```

and speaks line-delimited JSON over stdin/stdout. **iOS sandboxing forbids `fork`/`exec`.** There
is no workaround, no entitlement, no flag. The engine must be relinked as a static library or
framework and driven in-process, with the `BufferedReader`/`OutputStreamWriter` pipe replaced by a
Kotlin/Native `cinterop` bridge.

The encouraging part: we would be *reverting* a patch, not inventing something. Per
`docs/KATAGO_HUMAN_SL_IMPLEMENTATION_PLAN.md`, the `karino2` / PaooGo fork natively builds a JNI
**library**, and we deliberately patched `CMakeLists.txt` and `main.cpp` to emit a standalone
executable instead. The library form we need already exists upstream; what is missing is an
embeddable entry point for the analysis-engine main loop rather than a `main()`.

### What the Eigen backend buys us

Our binaries are built with the **Eigen (CPU) backend**, not OpenCL
(`docs/KATAGO_HUMAN_SL_IMPLEMENTATION_PLAN.md:61`). The `openclDeviceToUseThread0..4` lines still
sitting in `app/src/main/assets/katago.cfg` are inert leftovers from a stock KataGo config, not
evidence of a GPU build. This removes what would otherwise be the second-hardest problem:

- **Backend portability is already solved.** Eigen is header-only, CPU-only, ISO C++ with its own
  NEON vectorization. It compiles for `ios-arm64` and `ios-simulator-arm64` with stock clang. No
  CoreML or Metal conversion is needed for a first version.
- **No OpenCL auto-tuner.** KataGo's OpenCL backend runs a device-specific tuning pass on first
  launch and caches the result - a startup-latency and user-support burden we simply do not have.
- **It runs in the Simulator.** A GPU backend would not. The feature stays developable and
  CI-testable without a device.
- **The performance bar is already set by Android, and Apple hardware clears it.** We ship this
  exact CPU path on Android phones today, with `numSearchThreadsPerAnalysisThread` pinned to
  `Runtime.getRuntime().availableProcessors()` (`KataGoAnalysisEngine.kt:47`) and deliberately low
  visit counts - the human-SL feature samples `humanPolicy` rather than searching deeply. Apple
  cores are not slower than the Android median, so iOS would not be a downgrade on what users get
  now.
- **Simpler packaging.** Header-only Eigen, no GPU driver linkage, no extra system frameworks - a
  clean static library / xcframework.

### What still stands

- **Memory, and this is the new one.** On Android the engine is a *separate process* with its own
  address space and OOM budget. In-process on iOS it competes with the Compose UI under a stricter
  per-app jetsam limit: 35 MB net, 94 MB human net, `nnCacheSizePowerOfTwo = 15`, plus the search
  tree. Measure this first in any spike - it is the most likely thing to kill the feature.
- **Core topology.** `availableProcessors()` on Apple silicon counts performance *and* efficiency
  cores; scheduling 8 search threads across E-cores will underperform, and iOS throttles harder on
  thermals. Needs `activeProcessorCount` tuning rather than a straight port of the current logic.
- **Asset size.** `app/src/main/assets` is 265 MB - `katago_human.net` (94 MB) plus `katago.net`
  (35 MB). On iOS this needs **On-Demand Resources**, not bundling.
- **No build recipe in this repo.** The binaries are checked into git
  (`jniLibs/arm64-v8a/libkatago.so` 4.9 MB, `armeabi-v7a` 3.8 MB), there is no x86/x86_64 build, and
  the feature consequently already does not run on Android emulators. Also note there is no
  `LD_LIBRARY_PATH` / `nativeLibraryDir` equivalent on iOS - everything links statically.

**Recommendation: put local AI behind `expect`/`actual` and ship iOS v1 without it**, and track the
in-process relink as its own spike gated on a memory measurement. The Eigen backend means this is
one hard problem plus packaging, not three stacked hard problems - a credible follow-up rather than
a research project. Whether it belongs in v1 is a product decision, called out in section 8.

---

## 6. Phased plan

Every phase ends with a shippable Android build.

### Phase 0 - de-coupling (Android-only, zero KMP risk)

This phase introduces no multiplatform tooling at all and is worth doing on its own merits.

- Introduce `Logger` / `CrashReporter` / `Analytics` interfaces; migrate the 68 `android.util.Log`
  sites and ~95 `FirebaseCrashlytics.getInstance()` sites onto them. Note that a *domain model*
  (`data/model/local/Game.kt:142`) and a *DAO* (`GameDao.kt`) currently log to Crashlytics.
- Remove the 11 direct `OnlineGoApplication.instance` reads and the `GlobalContext.get()`
  service-locator sites; make every dependency constructor-injected.
- Moshi to kotlinx.serialization.
- `java.time` to kotlinx-datetime; drop core library desugaring.
- `org.json` to kotlinx.serialization in `OGSWebSocketService`.
- Coil 2 to Coil 3; navigation routes to `@Serializable` types.
- Split `utils/Globals.kt` into pure logic and `Resources`-dependent formatting.
- Fix layering violations: `RulesManager` imports `ui.screens.game.Variation`;
  `UserStats` depends on MPAndroidChart; `PuzzleDirectoryAction` / `TsumegoAction` carry
  `android.graphics.Point`; `BoardTheme` carries `androidx.compose.ui.graphics.Color` and
  `@StringRes`.
- Delete dead weight: `buildFeatures { viewBinding = true }` (unused), the empty
  `fileTree("libs")` dependency, `res/anim/` (Fragment-era leftovers), the orphaned
  `ui/screens/game_legacy/GamePresenterTest.kt`, `rang.hpp`, the unused
  `NOT_CHARGING_PERIOD_MINUTES` constants.
- Convert the two remaining Java files to Kotlin.

### Phase 1 - extract `:shared` (one module, still Android-only)

Create a single `:shared` library module and move the non-UI code into it (`model`, `logic`,
`network`, `database`, `data` packages), leaving `:app` with the Activity, notifications,
WorkManager, billing and Play services. UI stays in `:app` until phase 4.

This is the only structural split in the plan. It surfaces every remaining dependency cycle before
KMP can complicate the diagnosis, and it is the boundary that stops shared code reaching for `R`
and the Activity. Everything after this phase is source-set movement inside `:shared`, not new
modules.

### Phase 2 - make `:shared` multiplatform

Apply the KMP plugin to `:shared` with `androidTarget` + `iosArm64` / `iosSimulatorArm64` /
`iosX64`. Everything starts in `androidMain`; nothing has to move on day one and Android keeps
building.

Then move the `model` and `logic` packages to `commonMain` - ~950 LOC, enough to prove the
toolchain end to end without betting much on it. Move `RulesManagerTest` to `commonTest` (44 LOC
today; expand it, since it is about to become the shared correctness guarantee for two platforms).
Decide the score-estimator question (5.1) here.

### Phase 3 - data layer to `commonMain`

Still inside `:shared`, still no new modules. `network` to Ktorfit/Ktor; `database` to Room KMP and
DataStore KMP; `data` repositories to `commonMain`, one file at a time. The session/cookie/CSRF
layer needs `expect`/`actual`. The OGS websocket protocol (601 LOC in `OGSWebSocketService` + 312 in
`GameConnection`) can move once `org.json` and the JVM atomics are gone.

### Phase 4 - Compose Multiplatform (the stretch goal)

Move the UI from `:app` into `:shared/commonMain` - still two Gradle modules. Resources migration;
icons; the chart rewrite; the markdown rewrite; `ScrollableDropDownMenu`; and the board renderer.
`ui/composables/BoardComposable.kt` (794 LOC) is mostly portable already - it has exactly four
Android leaks: `nativeCanvas.drawText` for coordinate labels and move numbers, `MotionEvent` +
`pointerInteropFilter` for touch, `android.graphics.Rect` for text measurement, and `colorResource`.
All four have direct Compose-multiplatform equivalents.

Doing the resource migration here, against one module, is what keeps it to a single generated `Res`
class and a single Crowdin path.

### Phase 5 - iOS app

SwiftUI shell hosting `ComposeUIViewController`; notifications; background refresh; billing;
Google sign-in; App Store submission plumbing. Bitrise needs a macOS stack.

---

## 7. Risks

| Risk                                                       | Notes                                                                                                                                                                                                                                                                                                                                                                                                          |
|------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Toolchain currency**                                     | Kotlin 2.3.20 and AGP 9.2.1 are both very recent. Verify KMP plugin, Compose Multiplatform, Room KMP and Ktorfit compatibility - and that Gradle configuration cache (`org.gradle.unsafe.configuration-cache=true`) still holds - before committing to any of this. Keeping to three modules (section 3) confines this to one build file rather than nine, which is a large part of why that shape was chosen. |
| **Room KMP on a live schema**                              | v20 with 4 auto-migrations and 13 type converters, one of which serializes `MoveTree` through its own private Moshi instance. `fallbackToDestructiveMigration(dropAllTables = true)` limits the blast radius but means a bad migration silently wipes user caches.                                                                                                                                             |
| **Localization regression**                                | 663 strings x 16 locales, moved wholesale. Needs a mechanical diff check, not eyeballing.                                                                                                                                                                                                                                                                                                                      |
| **iOS background behaviour**                               | The Android model is a guaranteed-ish 15-minute WorkManager poll. `BGAppRefreshTask` gives no such guarantee. Game-turn notifications on iOS will be materially worse without server push.                                                                                                                                                                                                                     |
| **Asset size**                                             | 265 MB of KataGo weights. Even with local AI scoped out of iOS v1, the Android AAB situation does not improve.                                                                                                                                                                                                                                                                                                 |
| **In-process memory (iOS, if local AI is ever attempted)** | The subprocess model gave KataGo its own address space and OOM budget. In-process under iOS jetsam limits it shares one with the Compose UI. A new class of risk that does not exist on Android today; gate the spike on measuring it.                                                                                                                                                                         |
| **Bandwidth**                                              | This is a multi-month refactor on a project described in its own README as a personal playground. Phases 0 and 1 are the hedge: they pay off standalone if the rest is never done.                                                                                                                                                                                                                             |

---

## 8. Open decision

**Does iOS v1 ship without local AI?**

Everything in phases 0-4 is executable without an answer. This question changes the scope of phase 5
materially, and it is a product call rather than a technical one. The technical recommendation is
yes - ship without it, and treat in-process KataGo on iOS as a separate spike with its own
feasibility gate.

---

## 9. Rough effort

Order-of-magnitude only, for a single developer, assuming the codebase as it stands.

| Phase                            | Estimate                                            |
|----------------------------------|-----------------------------------------------------|
| 0 - de-coupling                  | 4-6 weeks                                           |
| 1 - extract `:shared`            | a few days                                          |
| 2 - make `:shared` multiplatform | 1-2 weeks (+1-2 if porting the estimator to Kotlin) |
| 3 - data layer to `commonMain`   | 4-6 weeks                                           |
| 4 - Compose Multiplatform        | 6-10 weeks (resources dominate)                     |
| 5 - iOS app                      | 4-8 weeks, excluding local AI                       |

Phases 0 and 1 deliver value regardless of whether the migration continues.

Note that settling on three modules instead of nine saves real time in phase 1, but it is a modest
slice of the total - the estimate is dominated by resources, serialization and Firebase
de-coupling, none of which care how the code is split across build files. The stronger argument for
three modules is risk, not effort: one build file to keep working against a brand-new toolchain
instead of nine.

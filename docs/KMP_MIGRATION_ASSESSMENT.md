# Kotlin Multiplatform migration - feasibility assessment

Scope of this document: what it would take to move OnlineGo to Kotlin Multiplatform with **iOS as
the second target**, and **Compose Multiplatform as the intended end state**. It identifies the
blockers, names a concrete replacement for every dependency that is not KMP-ready, and sequences
the work so that every phase still ships on Android.

This is an assessment, not a commitment. Measured against the repository at commit `3f1d980`.

**Status.** Five items have been built, all in Phase 0. The Moshi to kotlinx.serialization
migration, together with the `org.json` removal that was entangled with it (4.5); the jsoup
removal (4.6); Coil 2 to Coil 3 (4.7); Retrofit to Ktorfit (4.8); and PersistentCookieJar to a
Ktor-level session cookie store (4.9). All are green on unit tests and both build variants, and
**under on-device testing - not released**. Everything else here is still a plan.

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
2. ~~**Moshi to kotlinx.serialization**~~ - **done**, see 4.5. Cost far less than the annotation
   count suggested and far more than the *behavioural* analysis suggested; the surprises were
   Moshi's numeric leniency and its `Any` mapping, not the DTO work.
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

| Current                                                                                  | Why it fails                                                                                                                                 | Recommended replacement                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ~~**Retrofit 3.0.0**~~ **DONE**                                                          | JVM/Android only                                                                                                                             | **Ktorfit** over **Ktor 3**. Ktorfit is KSP-based and keeps the annotated-interface style, so `OGSRestAPI.kt` (33 endpoints, all already `suspend`) migrates close to 1:1. Engines: `OkHttp` on Android, `Darwin` on iOS. `CustomConverterFactory` (the hand-rolled TSV parser for the Glicko2 rating history) becomes a Ktorfit converter.                                                                                                                                                                                                                                                                              |
| **OkHttp 5.3.2** (direct use) - **mostly DONE**                                          | 5.x has KMP artifacts but constrained; our usage is deeply Android-flavoured                                                                 | Keep as the *Android engine* under Ktor. The network interceptor is gone (4.9): referer, CSRF and request logging are Ktor plugins, and the JWT branch turned out to be dead code. `HTTPConnectionFactory` is now only `EmulatorDnsSelector` (~18 lines of `android.os.Build` fingerprint checks), `followRedirects(false)` and the debug logging interceptor - all genuinely engine-level and Android-only. What remains is the websocket, which still takes the `OkHttpClient` directly.                                                                                                                               |
| ~~**Moshi 1.15.2, reflective**~~ **DONE**                                                | `KotlinJsonAdapterFactory` needs `kotlin-reflect`; no multiplatform equivalent                                                               | **kotlinx.serialization**. The annotation work is small - 7 `@Json(name=)` become `@SerialName` and the rest matches on field name - but the behavioural defaults differ in ways that fail at runtime, not compile time. **See 4.5.** Custom adapters (`OGSInstantJsonAdapter`, `OGSBooleanJsonAdapter`, `HashMapOfCellToStoneTypeMoshiAdapter`, `AiDifficultyMoshiAdapter`, `ResponseBriefMoshiAdapter`) become `KSerializer`s; the `PolymorphicJsonAdapterFactory` for `TutorialStep` becomes a sealed hierarchy with `@SerialName`. Consolidate the 6 scattered `Moshi.Builder()` instances into one injected `Json`. |
| ~~**`org.json`**~~ **DONE**                                                              | Android-only (AOSP-bundled)                                                                                                                  | Replaced with kotlinx.serialization `JsonArray` / `JsonElement`, and removed from the five other files that had picked it up (`utils/Globals.kt`'s emit DSL, `ServerNotificationsRepository`, `MyGamesViewModel`, `Challenge`, `OnboardingViewModel`). `grep -rn "org.json" app/src/main` is now empty.                                                                                                                                                                                                                                                                                                                  |
| ~~**PersistentCookieJar** (`com.github.franmontiel`)~~ **DONE**                          | backed by Android SharedPreferences                                                                                                          | Replaced by `OGSCookieStore`, a ~55-line `CookiesStorage` behind Ktor's `HttpCookies` plugin. Persistence stayed on SharedPreferences rather than DataStore, behind a `SessionCookiePersistence` seam - see 4.9 for why. Existing sessions are migrated, so no one is signed out by the upgrade.                                                                                                                                                                                                                                                                                                                         |
| ~~**Coil 2.7.0**~~ **DONE**                                                              | 2.x is Android-only                                                                                                                          | Now Coil 3.6.3 + `coil-network-okhttp`. Six files. See 4.7 - it was mechanical, but it forced compileSdk 37 and a Compose bump, and the avatar work alongside it was not mechanical at all.                                                                                                                                                                                                                                                                                                                                                                                                                              |
| **MPAndroidChart v3.1.0**                                                                | Android-only, **and it has leaked out of the UI layer** into `data/model/local/UserStats.kt` and `usecases/GetUserStatsUseCase.kt`           | **Vico 2.x** (has KMP targets) or **KoalaPlot**, or a hand-rolled Compose `Canvas`. Fix the layering violation first - `UserStats` is a domain model and must not depend on a charting library. `ui/screens/stats/ChartWrapper.kt` is a 416-LOC rewrite.                                                                                                                                                                                                                                                                                                                                                                 |
| **Markwon 4.6.2**                                                                        | renders into an `android.widget.TextView` via `AndroidView`                                                                                  | **`com.mikepenz:multiplatform-markdown-renderer-m3`**. Contained to one screen (`JosekiExplorerUI.kt`, ~90 LOC).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ~~**jsoup 1.22.1**~~ **DONE**                                                            | pure JVM - survives Android and Desktop, breaks iOS                                                                                          | Deleted. It was one 3-line `parseHtml()` in `TsumegoViewModel`; replaced by `AnnotatedString.fromHtml()` from Compose UI, which was already on the classpath. See 4.6.                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **`material-icons-extended`**                                                            | deprecated Android artifact; 109 references in 21 files                                                                                      | Vendor the icons actually used as `ImageVector` declarations (best for binary size), or use `br.com.devsrsouza:compose-icons`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| **navigation-compose 2.9.7** (androidx)                                                  | Android artifact                                                                                                                             | `org.jetbrains.androidx.navigation:navigation-compose`. Take the opportunity to convert the 13 string-literal routes in `Navigation.kt` to `@Serializable` type-safe routes - we will already have kotlinx.serialization on the classpath, and the current bottom-bar logic matches routes against a hardcoded `listOf("myGames", "learn", "stats", "settings")`.                                                                                                                                                                                                                                                        |
| **Firebase Crashlytics / Analytics** - **logging DONE**                                  | Google Android SDK                                                                                                                           | Logging is done (4.10): every `FirebaseCrashlytics.log(...)` now goes through **Kermit**, and Crashlytics is just one of its writers. What remains is the non-logging surface - `recordException` (10 direct sites that bypass the `utils/Crashlytics.kt` filter), `setCustomKey` (9), `setUserId`, `sendUnsentReports` (3) - plus `FirebaseAnalytics` (9 files). That is the `CrashReporter` / `Analytics` seam; Android actual delegates to Firebase, iOS to the Firebase iOS SDK (CocoaPods) or GitLive's `firebase-kotlin-sdk`.                                                                                      |
| ~~**`android.util.Log`** (68 sites, 26 files)~~ **DONE**                                 | Android-only                                                                                                                                 | **Kermit** (`co.touchlab:kermit`). Done together with the Crashlytics breadcrumbs - see 4.10. `grep -rn android.util.Log app/src/main` is empty; only `androidTest` still uses it.                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **`java.time`** (16 files)                                                               | JVM; currently works on `minSdk 23` only via core library desugaring                                                                         | **kotlinx-datetime**. Lets us drop `coreLibraryDesugaring` and `desugar_jdk_libs` entirely.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| **`java.text.SimpleDateFormat`** (3 files)                                               | JVM                                                                                                                                          | kotlinx-datetime `DateTimeFormat`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **`java.util.concurrent`** - `ConcurrentHashMap`, `AtomicInteger/Boolean/Long` (5 files) | JVM                                                                                                                                          | `kotlin.concurrent.Atomic*` from the stdlib; `ConcurrentHashMap` in `OGSWebSocketService` becomes a plain map guarded by a `Mutex`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| **`java.util.UUID` / `Stack` / `LinkedList` / `Locale`**                                 | JVM                                                                                                                                          | `kotlin.uuid.Uuid`, `ArrayDeque`, `expect`/`actual` for locale.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **`BuildConfig`** (`BASE_URL`, `DEBUG`)                                                  | AGP-generated                                                                                                                                | **BuildKonfig**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| **WorkManager 2.11.2**                                                                   | Android-only. One `CoroutineWorker` (`SynchronizeGamesWork`, 15-min periodic poll) delegating to `CheckNotificationsTask`                    | `expect`/`actual` `BackgroundSync`. Android keeps WorkManager; iOS gets `BGAppRefreshTask`, which is **best-effort with no interval guarantee**. Flag this honestly: the correct answer for iOS is server push (APNs), which OGS would have to support. The app currently has no FCM at all - background updates are poll-only.                                                                                                                                                                                                                                                                                          |
| **`NotificationUtils.kt`** (348 LOC)                                                     | `NotificationManager`, `PendingIntent`, `RemoteViews`, and it **rasterizes a board by instantiating the legacy `BoardView`** into a `Bitmap` | `expect`/`actual` notification surface; iOS `UNUserNotificationCenter`. Re-implement the board rasterization on top of the Compose renderer (`ImageBitmap` + `Canvas`) - that deletes `ui/views/BoardView.kt` (737 LOC) and `res/layout/notification_board.xml`, the last legacy View and the last XML layout in the project.                                                                                                                                                                                                                                                                                            |
| **Play Billing 9.1.0**                                                                   | Android-only                                                                                                                                 | `expect`/`actual`; iOS StoreKit. Consider **RevenueCat's KMP SDK** / `purchases-kmp` to avoid writing two store integrations. Contained to `playstore/PlayStoreService.kt` + `SupporterViewModel`.                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **Play In-App Review 2.0.2**                                                             | Android-only                                                                                                                                 | `expect`/`actual`; iOS `SKStoreReviewController`. Contained to `utils/ReviewPromptManager.kt` + `ReviewPromptRepository`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| **play-services-auth 21.5.1**                                                            | Android-only                                                                                                                                 | Credential Manager on Android; `GoogleSignIn` SDK or `ASWebAuthenticationSession` on iOS. Low risk - the token exchange itself is already plain REST against OGS.                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ~~**`gms.common.util.IOUtils`** in `HTTPConnectionFactory`~~ **DONE**                    | gratuitous Play Services dependency in the networking layer                                                                                  | Deleted with the interceptor it lived in (4.9). The gzip handling existed only because the log ran in an OkHttp *network* interceptor, below transparent decompression; at the Ktor layer `bodyAsText()` is already decoded.                                                                                                                                                                                                                                                                                                                                                                                             |
| **`AppLocaleManager`**                                                                   | `LocaleManager`, `LocaleList`, `Resources.getSystem()`, `attachBaseContext` wrapping, synchronous SharedPreferences                          | `expect`/`actual`. iOS sets `AppleLanguages` in `UserDefaults` and requires a restart.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Android string / drawable resources**                                                  | `R.*`                                                                                                                                        | **Compose Multiplatform resources** (`composeResources/`). It consumes the *same* `strings.xml` format, so Crowdin only needs a path change in `crowdin.yml`. Preferred over moko-resources, which is effectively in maintenance. See 4.4.                                                                                                                                                                                                                                                                                                                                                                               |
| **Mockito** (8 test files, 37 tests)                                                     | JVM-only                                                                                                                                     | Leave those tests in `androidUnitTest`. New `commonTest` coverage uses hand-written fakes plus Turbine and `kotlinx-coroutines-test`, both of which are already multiplatform.                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| **`com.android.compose.screenshot`**                                                     | AGP-only tooling                                                                                                                             | Keep it on the Android target. Roborazzi is the option if we later want screenshot tests shared across JVM targets.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |

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

### 4.5 Moshi to kotlinx.serialization - DONE, and what it actually took

Built on branch `kotlinx-serialization-migration`: 86 files, Moshi and `org.json` both gone from
`app/src`, unit tests up from 37 to 63, debug and R8 release builds green. Under on-device testing,
not released.

The annotation work was the small part, exactly as predicted - 7 `@Json(name=)` became
`@SerialName`, and field-name matching carried the rest. Everything that cost real time was a
behavioural difference that fails at runtime rather than compile time.

**The shipped configuration.** One `appJson` (`utils/AppJson.kt`) replaced all six
`Moshi.Builder()` instances. Every flag reproduces a specific Moshi behaviour:

```kotlin
Json {
  ignoreUnknownKeys = true   // OGS returns fields we do not model; kotlinx throws by default
  explicitNulls = false      // missing key decodes to null; nulls omitted on encode
  encodeDefaults = true      // Moshi wrote every non-null field regardless of default
  isLenient = true           // quoted numbers, which tutorials.json and older OGS rely on
}
```

`explicitNulls = false` is what let the 129 nullable-without-default properties keep working with
no source edits, which was the single highest-leverage decision in the change.

**Seven serializers**, in `data/ogs/` and `utils/serializers/`: `AnySerializer`,
`OGSBooleanSerializer`, `OGSInstantSerializer`, `LenientIntSerializer`, `LenientLongSerializer`,
`AiDifficultySerializer`, `ResponseBriefSerializer`. The first five are applied through
`@file:UseSerializers` on the DTO files that need them rather than per property, so a field added
later is covered automatically - the failure mode being guarded against is a new field that throws
only on real data.

#### What the assessment got wrong

**`Any`-typed fields were the biggest miss.** kotlinx has no `Any` serializer, and 13 DTO fields
are `Any` / `Any?` / `List<Any>` / `Map<String, Any>`. Worse, the *runtime representation* is
load-bearing: `Time.fromMap` casts to `Double`, `Game` does
`(white as? Map<*,*>)?.get("ratings") ... as? Double`, `Message.fromChat` casts to `String`.
`AnySerializer` reproduces Moshi's mapping exactly - number to `Double`, object to `LinkedHashMap`,
array to `ArrayList` - so none of those call sites had to change. Converting the fields to
`JsonElement` would have been cleaner and would have rippled through three more files.

**Moshi's numeric leniency was missed entirely, and it reached production testing.** OGS returns
`"order": 99999999.0` for an `Int?`. Moshi's `nextInt()` accepted a float whose value was exactly
integral; kotlinx's `decodeInt` rejects the `.`, and `isLenient` does not help because it only
relaxes quoting. This crashed the puzzle directory on device. Fixed with
`LenientIntSerializer` / `LenientLongSerializer` - exactly-integral floats and quoted numbers
accepted, genuinely fractional values rejected, matching Moshi in both directions.

**The "three fields need checking by hand" worry was unfounded, in the opposite direction to the
one predicted.** With `explicitNulls = false`, an *explicit* JSON `null` against a nullable
property that has a non-null default stays `null` - it does not fall back to the default. That is
exactly what Moshi did. Pinned in `NullableDefaultsTest` rather than left to reasoning.

**kotlinx serializes class-body properties; reflective Moshi bound only constructor parameters.**
Two needed `@Transient`: `OGSClock.receivedAt` (set after parsing) and `AiGameState.chatText`
(a string-resource id that must never be persisted). The second is a small behaviour change worth
noting - a legacy blob now drops its stale resId on *read* rather than on write, which is what the
existing test was asserting the intent of anyway.

**`StoneType` had to become Kotlin.** It was a Java enum, which the kotlinx compiler plugin cannot
see at all.

**`TutorialStep`'s discriminator collided with a real property.** `sealed class TutorialStep(val
type: String)` could not keep `type`, since kotlinx uses it as the class discriminator - and its
default discriminator is already `"type"`, so removing the property and adding `@SerialName` on
each subtype was the whole fix. The wire labels stay `Interactive` / `Lesson` / `Game`.

**Three things turned out to be dead** and were deleted rather than ported:
`HashMapOfCellToStoneTypeMoshiAdapter` (registered but nothing in `AiGameState` or `Position` is
that type), `emitWithResponse` with its `pendingRequests` map, and `OGSRestService`'s injected
`moshi` property.

#### Persistence

`DbTypeConverters.moveTreeAdapter` and `PersistenceManager` were the two sites holding
Moshi-written data. `UIConfig` now has a parse-failure fallback, so a bad blob costs a re-login
rather than a launch crash. The planned Room version bump to force a destructive wipe of
`MoveTree` blobs was applied and then **deliberately reverted** - the preference is to let a stale
row throw and be seen. Worth knowing where that surfaces: inside a Room type converter, so the
stack trace points at the DAO rather than at a decode boundary.

#### Outstanding

The CI check that every `data/model` file declaring a `Boolean` / `Int` / `Long` also carries the
matching `@file:UseSerializers` annotation was specified but **not built**. Without it, the
guarantee that new fields are covered automatically holds only for fields added to existing
annotated files, not for a new file.

### 4.6 jsoup - DONE

A 508 KB jar for one function. `TsumegoViewModel.parseHtml()` called
`Jsoup.parseBodyFragment(body).body().text()` at exactly one site, to flatten a puzzle's
`puzzle_description` into the plain `Text` under the board. Nothing else in the repo imported it,
and most OGS descriptions carry no markup at all, so most of the time the parser ran for nothing -
and when it did fire it discarded the formatting rather than showing it.

Replaced by `AnnotatedString.fromHtml()` (`androidx.compose.ui.text`, already on the classpath at
ui-text 1.10.6). Bold, italics, lists and links now render instead of being stripped. Links are
styled primary + underlined via `TextLinkStyles`, because Compose makes `LinkAnnotation` clickable
automatically and an unstyled link would be an invisible tap target.

The conversion moved from the ViewModel to a single `remember`-cached helper at the render site in
`TsumegoUI`, which also picks up move-tree node text - rendered by the same `Text`, and never
parsed at all until now.

Three behavioural changes, none covered by a test:

- **Line structure survives.** jsoup's `.text()` collapsed `<p>` and `<br>` into single spaces, so
  a multi-paragraph description rendered as one run-on blob. `Html.fromHtml` emits newlines.
- **Trailing newlines.** `Html.fromHtml` leaves a trailing `\n` after a block element; jsoup never
  did. Hence the small `trimTrailingWhitespace()` on `AnnotatedString`.
- **Node text is now parsed**, so a node containing a literal `<` followed by a word can have it
  swallowed as a tag. Descriptions already carried that risk under jsoup; node text did not.

Also removed: the `-dontwarn com.google.re2j.**` ProGuard rule, which existed only because jsoup
carries optional refs to re2j. Verified first that jsoup resolved once on `releaseRuntimeClasspath`
with no transitive pullers and that re2j was not on the classpath at all, then confirmed by a
green `assembleRelease`.

Measured: dex 8,132,068 -> 7,980,216 bytes, APK 147,836,957 -> 147,365,719. The APK saving is
larger than the dex saving because jsoup's entity tables ship as jar resources.

**KMP effect.** This trades a JVM-only dependency for an Android-only API, so it is not a straight
win on paper. What it does buy is position: the blocker left the ViewModel, which is destined for
`commonMain`, and now sits in the UI layer, where it needs an `expect`/`actual` (or a markdown
renderer) at CMP time rather than blocking the shared code.

### 4.7 Coil 2 to Coil 3 - DONE

Upgraded to 3.6.3 with `coil-network-okhttp`; Coil 3 dropped built-in network support from
`coil-core`, so without that artifact nothing loads over HTTP at all. Six files, `coil.*` ->
`coil3.*`. `LocalContext.current` became `coil3.compose.LocalPlatformContext.current`, which is a
typealias for `Context` on Android and is the form that compiles in `commonMain` later.

Two things the assessment did not anticipate.

**It is not a self-contained upgrade.** Coil 3.5 onwards depends on Compose 1.12.0, which requires
compileSdk 37. So the version bump dragged `compileSdk` 36 -> 37 and the Compose BOM
2026.03.01 -> 2026.08.00 (UI 1.10.6 -> 1.12.0) along with it. Coil 3.4.0 was verified as a
working alternative that needs neither, and delivers the same behaviour, since everything that
mattered here landed in 3.0 - taking 3.6.3 was a deliberate choice, not a requirement. `targetSdk`
was deliberately left at 36; compileSdk and targetSdk move independently and there was no reason
to opt into new runtime behaviour in the same change. No new deprecation warnings appeared.

**`rememberAsyncImagePainter` changed behaviour silently.** Its default size resolver no longer
waits for the first draw to measure the canvas; it defaults to `Size.ORIGINAL`. Four of the six
sites used it, and left alone they would have decoded source-resolution bitmaps into 48-124dp
slots. All four moved to `AsyncImage`, which resolves from layout constraints. Both APIs default
to `ContentScale.Fit` and `Alignment.Center`, so there was no visual change - but this would have
been an invisible memory regression, not a compile error.

#### The avatar work, which was the actual point

The upgrade was the occasion; cached avatars being slow was the reason. Measuring the live
endpoints found the causes were mostly not Coil's:

| host                         | cache headers                              |
|------------------------------|--------------------------------------------|
| `secure.gravatar.com`        | `cache-control: max-age=300`, no ETag      |
| `user-uploads.online-go.com` | none at all; only `ETag` + `Last-Modified` |

A Gravatar avatar goes stale after five minutes, so Coil 2 issued a conditional GET before it
could draw - a full network round trip for bytes already on disk. **Coil 3 ignores `Cache-Control`
by default** and always writes to its disk cache, so the upgrade fixed this for free.

The bigger problem was in our own code. `processGravatarURL` computed
`max(512.0, 2.0.pow(ln(width) / ln(2.0)))` for CDN-hosted avatars. That exponent is an identity -
`2^(log₂ w)` is `w` - so the expression reduced to `max(512, width)`, which is **always 512** for
every call site in the app. A missing `ceil` suggests power-of-two bucketing was intended and the
512 floor then defeated it. The CDN serves 32/64/128/256/512 (verified consistent across four real
accounts), so the home header was downloading 532 KB and decoding a 512x512 bitmap to draw it at
64dp. It now rounds up to the smallest size that covers the display, which is never softer and is
usually an order of magnitude smaller.

The Gravatar branch was deliberately left on `?s=<exact px>`. Consequence: a Gravatar-hosted
avatar still has one URL per display size, so it is fetched separately for the header, settings,
the opponent list and the dialogs. Each is now fetched once and served from disk thereafter
instead of revalidating every five minutes, so it is much better than before, but it does not
share across screens the way CDN-hosted ones now do.

Also: `crossfade(true)` came off the four sites that had it. Coil skips the crossfade on a memory
hit but not on a disk hit, so every app restart faded the avatar in over 100 ms. And the user's
own avatar is now preloaded in the pre-warm block that already exists in
`OnlineGoApplication.onCreate`, so the home header has it before the screen is reached.
`HomeScreenHeader` exports the two sizes it uses so the preload produces the same cache key; note
those two sizes differ from each other (URL at 56dp, drawn at 64dp), which looks accidental and
was left alone.

No custom `ImageLoader` was added. Coil 3's defaults are already the aggressive-caching ones.

#### Follow-up: the Stats tab

On-device testing surfaced a delay on the Stats screen that looked like slow image loading and
mostly was not. `StatsState.Initial` carried `playerDetails = null`, and the avatar URL only
arrived with `restService.getPlayerProfileAsync` - so on a bottom bar tab that rebuilds its view
model on every visit, the avatar sat on the fallback drawable for a full network round trip before
the image request even started. By then the image itself was usually already cached. Removing the
crossfade did not cause this; it removed the fade that had been disguising it.

`StatsState` now carries its own `avatarURL`, seeded synchronously from
`userSessionRepository.uiConfig?.user?.icon` when no `playerId` argument is present, so viewing
your own stats draws the avatar on the first frame with no network at all. This mirrors what
`SettingsViewModel` already does.

Two things left as they are. The 124dp avatar in `BoxWithImage` is the only thing in the app that
lands on the 512 CDN bucket - everything else converges on 256 - so the first load for any given
player is structurally a cache miss. `crossfade(true)` was restored on that one composable to
soften it rather than shrinking the image; the other five sites stay instant with no fade. And the
*other player* Stats route still waits on its profile call, because there is no cached
per-player lookup to seed from - `PlayersRepository` exposes only `getRecentOpponents` and
`searchPlayers`. Passing the URL as a navigation argument from the dialog that already displayed
it would fix that cheaply if it ever becomes annoying.

---

### 4.8 Retrofit to Ktorfit - DONE

33 endpoints moved from Retrofit 3.0.0 to Ktorfit 2.7.5 over Ktor 3.5.0. The annotated-interface
style survived almost unchanged, as 4.2 predicted - paths, verbs, `@Path`/`@Query`/`@Body` and
their defaults are identical, and a method-set diff against the Retrofit version of
`OGSRestAPI` is empty. The interface keeps its name; only its annotations changed.
What cost the time was everything *around* the interface.

#### What the assessment got wrong

4.2 said `CustomConverterFactory` "becomes a Ktorfit converter", which was right, and implied the
rest was mechanical, which was not. Five things only showed up by running or reading the libraries:

- **`Ktorfit.Builder.baseUrl` rejects a URL without a trailing `/`** (a raw `endsWith` check, where
  Retrofit validates the parsed `HttpUrl`). `BASE_URL` gained the slash - which then produced `//`
  at three `BuildConfig.BASE_URL + "/..."` concatenations, and meant the ten `@GET("/...")` paths
  had to lose their leading slash, because Ktorfit concatenates where Retrofit resolves.
- **Ktorfit's `@Body` never sets `Content-Type`.** Ktor's ContentNegotiation refuses to serialize
  without one, so all eight body-bearing endpoints - login and register included - failed with
  `Fail to prepare request body for sending ... Content-Type: null`. Retrofit got this free from
  the converter factory's media type. Fixed with one `defaultRequest { contentType(...) }`.
- **Ktorfit's KSP processor crashes on `%` in a path.** The Google OAuth endpoint carried a
  pre-encoded `scope=...%3A%2F%2F...` query string; KotlinPoet reads `%` as a format specifier
  (`index 3 for '%3A' not in range`). Declaring the constants as `@Query` parameters with decoded
  defaults produces a byte-identical query string, which a test pins.
- **Ktor follows redirects at its own layer**, independently of the engine, so OkHttp's
  `followRedirects(false)` was not enough; the client needs `followRedirects = false` too. Both
  legs of the Google handshake depend on seeing their `302`.
- **`expectSuccess = true` raises 3xx as `RedirectResponseException`**, which would have broken
  those same two methods. They opt out through a defaulted `@ReqBuilder` parameter.

#### Two bugs found in the code being migrated

Neither was caused by the migration; both were found by checking the old behaviour against the
wire rather than assuming it was correct.

- **The Glicko2 TSV parser dropped the oldest game.** `Reader.readLines()` yields no trailing empty
  element, but the `.dropLast(2)` was written as though it did ("drop empty line at the end +
  initial rating"), so it removed the synthetic initial-rating row *and* one real game. The
  replacement drops that row by its actual marker (`game_id` 0) instead of by position, so the
  rating chart gains a point. The endpoint also answers `text/plain`, which is why it needs a
  Ktorfit `Converter.Factory` and cannot go through content negotiation at all.
- **`acknowledgeWarning` sent the wrong body.** It passed the literal `"{accept: true}"` as a
  `String`, which kotlinx serialized as a JSON *string* - and whose contents are not valid JSON
  either. The OGS web client sends `{"accept":true}` as an object
  (`AccountWarning.tsx:121`, `requests.ts`). Now a `@Serializable` request type. This had been
  broken on the Retrofit path too.

#### Error handling

`retrofit2.HttpException` reached ten files, including one site that *manufactured* one for the
"server returns 200 on a bad password" hack. It is replaced by `OGSApiException(code, errorBody)`,
produced by an `HttpResponseValidator` on the client, plus `Throwable?.httpStatusCode` and
`Throwable?.httpErrorBody` accessors so call sites name no HTTP library at all. The body has to be
captured when the exception is built, because Ktor's `bodyAsText()` is `suspend` and every catch
site here is not. That incidentally fixed a double `ResponseBody.string()` read in
`OnboardingViewModel`, where the second call always threw `closed` and silently blanked the
diagnostic.

#### Shape

Client configuration lives in `configureOGSClient(jsonFormat)` (`data/ogs/OGSHttpClient.kt`) rather
than inline in the Koin module, so the DI module and all four test classes share one definition.
That is what makes the tests worth having: `GoogleAuthRequestTest` exercises the real
`expectSuccess`/redirect configuration, so the `@ReqBuilder` opt-out is actually proven rather than
assumed.

`preconfigured = get<OkHttpClient>()` keeps the existing `OkHttpClient` as the engine. That was a
deliberate deferral at the time, because the cookie jar had three consumers with nothing to do with
Retrofit. **Resolved in 4.9** - everything in `HTTPConnectionFactory` except the engine-level
settings has since moved into the Ktor plugin layer.

#### Verification

76 unit tests green (13 new), debug, release and screenshot variants all build. Release was checked
beyond "it compiles": `Signature` is kept by AGP's default rules (needed for the generic
`typeInfo<PagedResult<OGSGame>>` lookups), Ktor's consumer rules are applied, and no Retrofit
artifact remains on `releaseRuntimeClasspath`. Retrofit's three R8 rules were removed with it.

**What the tests do not cover:** every endpoint now runs on a path that has never reached the live
server. The suite covers URL shapes, body encoding, error translation and the TSV parse - not the
OkHttp engine bridge. (Cookies and CSRF were uncovered when this was written; 4.9 added tests for
them.) The eight write endpoints are the ones to exercise first on device, since the `Content-Type`
defect would have broken all of them and no unit test would have caught it without a request going
out.

---

### 4.9 PersistentCookieJar to a Ktor cookie store - DONE

`PersistentCookieJar` (`com.github.franmontiel`, last released 2016) was an OkHttp `CookieJar`, so
it sat below the layer Ktorfit had just taken over, and it dragged three unrelated things down
there with it: the `x-csrftoken` header, the Crashlytics request log, and
`UserSessionRepository.isLoggedIn()`. Replaced by `OGSCookieStore`, a `CookiesStorage` behind
Ktor's `HttpCookies` plugin, plus a one-time import of the old jar's contents.

#### What is actually on the wire

Checked against the live server rather than inferred:

```
set-cookie: csrftoken=…; expires=…+1y; Max-Age=31449600;  Path=/; SameSite=Lax
set-cookie: sessionid=…; expires=…+5y; Max-Age=157800000; Path=/; SameSite=Lax; HttpOnly
```

Two cookies, no `Domain` attribute - so both are *host-only* for `online-go.com`. That answers a
question the port would otherwise have had to guess at: the websocket at `wss://wsp.online-go.com/`
shares the `OkHttpClient` but has never received these cookies, and authenticates with
`chat_auth`/`user_jwt` over the socket instead. Removing the OkHttp cookie jar could not affect it.
Neither cookie is `Secure`, which is why the legacy persistence keys read `http://online-go.com/|…`
and not `https://`.

#### What the assessment got wrong

- **"persisted through DataStore"** (4.2). DataStore loads asynchronously, and
  `UserSessionRepository.isLoggedIn()` is synchronous and runs on the cold-start path.
  `PersistentCookieJar` read SharedPreferences synchronously in its constructor, so going async
  would have introduced a cold-start race where a request could fire before the session loaded.
  The store keeps SharedPreferences, read once into memory at construction, behind a
  `SessionCookiePersistence` interface - which is both the JVM-test seam (
  `unitTests.isReturnDefaultValues`
  makes `SharedPreferences` unusable in unit tests) and the line the KMP port will cut along.
- **"the cookie jar has three consumers"** (4.8). One of them, `FacebookLoginCallbackActivity`, is
  dead code: `android:enabled="false"` in the manifest *and* an explicit
  `setComponentEnabledSetting(…, COMPONENT_ENABLED_STATE_DISABLED, …)` on every
  `MainActivity.onCreate` (`MainActivity.kt:171`). It was ported onto the shared `HttpClient` to
  keep it compiling, but nothing exercises it.
- **The `X-User-Info` / godojo branch in the interceptor never fired.** It tested
  `request.url.pathSegments.contains("godojo")`; no endpoint in `OGSRestAPI` has that segment - the
  joseki endpoint is `oje/positions`. Deleted rather than ported.

#### The migration, which is the whole point

Existing users must not be signed out, so the old jar's contents have to be readable *after* its
library is gone. `SharedPrefsCookiePersistor` wrote, into prefs file `CookiePersistence`, a
hex-encoded Java serialization stream whose custom `writeObject` emits
`name, value, expiresAt, domain, path, secure, httpOnly, hostOnly`.

`LegacyCookieImport.kt` reads that back with an `ObjectInputStream` whose `resolveClass` redirects
the old FQN onto a local shim. Two things had to be exactly right, and only one of them was
obvious:

- `serialVersionUID` must match the library's `-8594045714036645534L` or `ObjectInputStream` throws
  `InvalidClassException`.
- **`resolveClass` alone is not enough.** `ObjectStreamClass.initNonProxy` additionally compares
  names via `classNamesEqual`, which compares only the *simple* name. The shim therefore has to be
  called `SerializableCookie` - the package may differ, the class name may not. This failed first
  and the error names it precisely; worth knowing before writing the next one of these.

The import runs only when the new prefs file is empty, which makes it idempotent, and it writes
through immediately so it does not re-run on every cold start. Entries that fail to decode are
skipped rather than thrown.

**The old `CookiePersistence` file is deliberately left in place.** That is the rollback path - a
user who downgrades finds their session where the old build looks for it. Deleting it, along with
the shim and the import branch, is a separate contraction step once a release has shipped and
stuck. Doing both at once would remove the rollback this exists to provide.

#### Shape

`configureOGSClient` now takes the storage, the base URL and a `log: (String) -> Unit`, all
defaulted so the four existing Ktorfit test classes compile unchanged. `log` is a parameter rather
than a direct `FirebaseCrashlytics.getInstance()` call because that throws in plain JVM unit tests.

The `x-csrftoken` header is a `createClientPlugin` hook that reads the token back out of
`HttpCookies` via the public `HttpClient.cookies(url)` extension, rather than taking the store as a
parameter. One source of truth, no pipeline-ordering question, and the test classes exercise the
real header path against `AcceptAllCookiesStorage` for free.

Request logging split in two, which lands exactly one line per response: success goes in
`validateResponse`, errors in the `handleResponseExceptionWithRequest` that was already reading the
body to build `OGSApiException`. The ordering is not accidental - `HttpCallValidator` reverses its
validator list and `addDefaultResponseValidation()` registers last (`HttpClient.kt:1399`), so the
default non-2xx throw runs before our success log.

The store keeps only `csrftoken` and `sessionid`, and only for the OGS host. The host check is a
trust boundary rather than an optimisation: it is what stops a future absolute-URL endpoint from
leaking `sessionid` off-site. The host is derived from `BuildConfig.BASE_URL` so it cannot drift.

#### Verification

91 unit tests green (15 new), all four build variants. The migration test decodes a blob produced
by the real library - captured from it before the dependency was deleted - and asserts the exact
values and expiry timestamps, with the allowlist, non-persistent and corrupt-entry cases derived
from that same blob by patching its `TC_STRING` and `TC_BLOCKDATA` fields.

Release was checked past "it compiles", because the shim's `readObject` is only ever invoked
reflectively and nothing references `serialVersionUID`: both survive R8 in the release dex,
unrenamed and with the exact `(Ljava/io/ObjectInputStream;)V` signature, under the existing
`-keep class io.zenandroid.onlinego.data.** { *; }` rule. No `franmontiel` artifact remains on
`releaseRuntimeClasspath`.

**What the tests do not cover:** the migration itself, end to end on a device. The decoder is
proven against real bytes, but the SharedPreferences plumbing around it is not - that needs
installing the build at `73bb1ad`, logging in, then `adb install -r` of the new build *without
uninstalling*, and confirming the session survives both that and a subsequent force-stop. Also
uncovered: that the csrf header satisfies Django on a real unsafe request (accept or decline a
challenge), and Google sign-in end to end.

#### Deliberate behaviour differences

- Only `csrftoken` and `sessionid` are kept; anything else OGS sets is discarded. If OGS ever
  starts depending on a third cookie, login breaks and the symptom will not point at the cause -
  `KEPT_COOKIES` is the line to look at.
- `recordException("Possible cookie jar problem")` is gone. It was instrumentation for a bug in the
  library that has been deleted.
- The websocket handshake is no longer logged to Crashlytics - it goes through the OkHttp engine,
  below the Ktor plugin layer.
- 302s are no longer logged as errors. The old interceptor logged on `!response.isSuccessful`,
  which includes redirects.

### 4.10 Logging onto Kermit - DONE

Two logging systems ran side by side and rarely agreed: `android.util.Log` went to logcat only,
`FirebaseCrashlytics.log` went to Crashlytics breadcrumbs only, and many call sites wrote the same
event to both, by hand, with an `E/TAG:` prefix to fake a severity. Both are now one `Logger` call
(Kermit 2.2.0, multiplatform), configured in `OnlineGoApplication.onCreate`:

- `CrashlyticsBreadcrumbWriter` (`utils/Crashlytics.kt`, ~10 lines) - forwards **Info and above**
  to `FirebaseCrashlytics.log` as `I/Tag: message`. It never calls `recordException`; non-fatals
  stay explicit at the call site. Installed in every build.
- `platformLogWriter()` - logcat on Android, `os_log` on iOS when we get there. **Debug builds
  only.**
- Release also sets the global minimum severity to Info, and every Verbose/Debug call uses Kermit's
  lambda overload (`Logger.d(tag = …) { "…" }`), so in release those messages are never even
  built. That replaced the `if (BuildConfig.DEBUG)` gates in `OGSWebSocketService`, whose
  debug-only lines became Debug level.

Kermit's own `kermit-crashlytics` was considered and rejected: it pulls in CrashKiOS and, by
default, records a non-fatal for every `Logger.e` carrying a throwable - a silent jump in reported
volume. Writing the writer ourselves keeps that decision explicit.

The severity cut-off is what preserves today's split. Every former `FirebaseCrashlytics.log` call
became `i`/`w`/`e` (the `E/`/`W/`/`I/` prefixes became the level), so every breadcrumb that reached
Crashlytics still does. Former `Log.v`/`Log.d` calls stayed `v`/`d`, so they still do not. Where a
site logged the same event to both, the pair became one call at Info or above.

`configureOGSClient` lost its `log: (String) -> Unit` parameter - it was a seam for exactly this,
and the request log now calls `Logger` directly with tag `HTTP_REQUEST`. `FaceToFaceViewModel`
lost its injected `FirebaseCrashlytics`, which it only used for `log`.

#### Verification

93 unit tests green; debug and release compile. JVM unit tests need no logging setup: Kermit's
default writer is logcat, and `unitTests.isReturnDefaultValues = true` already turns those calls
into no-ops.

#### Deliberate behaviour differences

- **Release builds write nothing to logcat.** Previously every `Log.*` call did. Debugging a
  release build means reading the Crashlytics breadcrumbs, or flipping the writer list locally.
- Release no longer pays for building Verbose/Debug strings - notably the `AiMoveDebug` line,
  which sorted and formatted every candidate move on each AI turn just to log it.
- `GameViewModel.onUserAction` no longer logs `BoardCellDragged`, which fired on every drag event
  and flooded the breadcrumb buffer. Discrete actions are still logged.
- Former logcat-only `Log.e` / `Log.w` / `Log.i` calls now also reach Crashlytics breadcrumbs. The
  64 KB breadcrumb buffer has a little more competition; the noisy ones (raw websocket frames,
  ClockDrift, notifications polling) were verbose/debug and stay out.
- The three websocket connect/close/failure lines used to be DEBUG-only in logcat with a separate,
  terser Crashlytics breadcrumb. They are now one line each, and the close breadcrumb gains the
  code and reason. "Received unexpected response" was DEBUG-only and is now a Warn breadcrumb in
  release; it should never fire, so if it does we want to know.
- A few logcat levels were corrected while merging pairs: "Setup Billing Done" was `Log.e`, now
  Info; "Billing client Disconnected" was `Log.e`, now Warn.

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

- ~~Logging~~ - **done**, see 4.10: `android.util.Log` and `FirebaseCrashlytics.log` are both on
  Kermit, including the domain model (`Game.kt`) and DAO (`GameDao.kt`) sites. Still owed:
  `CrashReporter` / `Analytics` seams for the remaining 23 non-logging Crashlytics calls and the
  `FirebaseAnalytics` usage in 9 files.
- Remove the 11 direct `OnlineGoApplication.instance` reads and the `GlobalContext.get()`
  service-locator sites; make every dependency constructor-injected.
- ~~Moshi to kotlinx.serialization~~ - **done**, see 4.5. Done as a single cut-over rather than
  the staged sequence planned here, and without the golden-JSON corpus: OGS responses have drifted
  over the years, so a captured corpus would not have been representative and faithful reproduction
  of Moshi's *leniency* became the correctness goal instead. That trade is what pushed the numeric
  leniency bug (4.5) out to on-device testing rather than catching it in CI.
  Adding `= null` to the 129 nullable-without-default properties remains worthwhile cleanup, in
  separate commits - the `Json` config already covers the behaviour, so this is tidying, not a fix.
- ~~`org.json` to kotlinx.serialization~~ - **done**, and wider than scoped here: it had spread to
  five files beyond `OGSWebSocketService`.
- Build the CI annotation check described at the end of 4.5. Outstanding.
- ~~jsoup~~ - **done**, see 4.6. One 3-line function, replaced by an API already on the classpath;
  461 KB off the APK.
- `java.time` to kotlinx-datetime; drop core library desugaring.
- ~~Coil 2 to Coil 3~~ - **done**, see 4.7. Navigation routes to `@Serializable` types outstanding.
- ~~PersistentCookieJar~~ - **done**, see 4.9. Took the referer/CSRF/logging interceptor and the
  `gms.common.util.IOUtils` helper with it. A contraction step is still owed: once a release has
  shipped, delete the legacy `CookiePersistence` prefs file and the import shim.
- Split `utils/Globals.kt` into pure logic and `Resources`-dependent formatting.
- Fix layering violations: `RulesManager` imports `ui.screens.game.Variation`;
  `UserStats` depends on MPAndroidChart; `PuzzleDirectoryAction` / `TsumegoAction` carry
  `android.graphics.Point`; `BoardTheme` carries `androidx.compose.ui.graphics.Color` and
  `@StringRes`.
- Delete dead weight: `buildFeatures { viewBinding = true }` (unused), the empty
  `fileTree("libs")` dependency, `res/anim/` (Fragment-era leftovers), `rang.hpp`, the unused
  `NOT_CHARGING_PERIOD_MINUTES` constants. Note `ui/screens/game_legacy/GamePresenterTest.kt` is
  **not** dead despite the orphaned package name - it still covers `formatMillis` and the boolean
  coercion against the real `User` DTO, and was repointed rather than deleted in 4.5. Rename it
  instead.
- ~~Convert the remaining Java file to Kotlin~~ - **done**: `utils/AndroidLoggingHandler.java` was
  unreferenced, so it was deleted rather than converted (4.10). `StoneType.java` went in 4.5. The
  project has no Java left.

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
`GameConnection`) can move once the JVM atomics are gone - `org.json` already is.

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

| Risk                                                       | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Toolchain currency**                                     | Kotlin 2.3.20 and AGP 9.2.1 are both very recent. Verify KMP plugin, Compose Multiplatform, Room KMP and Ktorfit compatibility - and that Gradle configuration cache (`org.gradle.unsafe.configuration-cache=true`) still holds - before committing to any of this. Keeping to three modules (section 3) confines this to one build file rather than nine, which is a large part of why that shape was chosen.                                 |
| **Room KMP on a live schema**                              | v20 with 4 auto-migrations and 13 type converters, one of which serializes `MoveTree` to JSON. `fallbackToDestructiveMigration(dropAllTables = true)` limits the blast radius but means a bad migration silently wipes user caches. Live concern today, not just for KMP: after 4.5 those blobs are read by a different serializer than wrote them, and the version was deliberately left at 20 so a stale row throws rather than being wiped. |
| **Localization regression**                                | 663 strings x 16 locales, moved wholesale. Needs a mechanical diff check, not eyeballing.                                                                                                                                                                                                                                                                                                                                                      |
| **iOS background behaviour**                               | The Android model is a guaranteed-ish 15-minute WorkManager poll. `BGAppRefreshTask` gives no such guarantee. Game-turn notifications on iOS will be materially worse without server push.                                                                                                                                                                                                                                                     |
| **Asset size**                                             | 265 MB of KataGo weights. Even with local AI scoped out of iOS v1, the Android AAB situation does not improve.                                                                                                                                                                                                                                                                                                                                 |
| **In-process memory (iOS, if local AI is ever attempted)** | The subprocess model gave KataGo its own address space and OOM budget. In-process under iOS jetsam limits it shares one with the Compose UI. A new class of risk that does not exist on Android today; gate the spike on measuring it.                                                                                                                                                                                                         |
| **Bandwidth**                                              | This is a multi-month refactor on a project described in its own README as a personal playground. Phases 0 and 1 are the hedge: they pay off standalone if the rest is never done.                                                                                                                                                                                                                                                             |

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
| 0 - de-coupling                  | 4-6 weeks; serialization slice done, see below      |
| 1 - extract `:shared`            | a few days                                          |
| 2 - make `:shared` multiplatform | 1-2 weeks (+1-2 if porting the estimator to Kotlin) |
| 3 - data layer to `commonMain`   | 4-6 weeks                                           |
| 4 - Compose Multiplatform        | 6-10 weeks (resources dominate)                     |
| 5 - iOS app                      | 4-8 weeks, excluding local AI                       |

Phases 0 and 1 deliver value regardless of whether the migration continues.

The serialization slice was estimated at 1.5-2.5 weeks, but most of that number was the
golden-JSON corpus, which was dropped. Skipping it did not remove the cost so much as move it -
out of CI and into the on-device testing tail, where the numeric-leniency bug surfaced. Treat the
remaining phase-0 estimate as roughly unchanged, and read the actual lesson as being about *where*
verification happens rather than how long the code takes.

Note that settling on three modules instead of nine saves real time in phase 1, but it is a modest
slice of the total - the estimate is dominated by resources and Firebase de-coupling, neither of
which cares how the code is split across build files. The stronger argument for three modules is
risk, not effort: one build file to keep working against a brand-new toolchain instead of nine.

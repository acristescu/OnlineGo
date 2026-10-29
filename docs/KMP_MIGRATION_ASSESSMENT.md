# Kotlin Multiplatform migration

Goal: OnlineGo on Kotlin Multiplatform with **iOS as the second target** and **Compose
Multiplatform** for all UI, without a long-lived branch - **the Android app builds and ships at the
end of every phase.**

**Status.** Phases 0-4 are done and shipped (`origin/release`, tag `beta_b1064`): the data layer,
all screens, navigation and the app shell live in `:shared/commonMain`, and `:app` is only the
Android shell. Phase 5 (the iOS app) has started: `iosApp/` runs the shared UI in the Simulator
with in-process logout, remote images, puzzle HTML, in-app language switching and the review
prompt. The remaining
platform bindings are stubbed. `docs/IOS_DEVELOPMENT.md` covers building and running it.

Slice numbers (6.x) in commit messages map to the full write-ups in earlier versions of this file:
`git show 9cf6cb29:docs/KMP_MIGRATION_ASSESSMENT.md` (6.1-6.56) and
`git show d54399dc:docs/KMP_MIGRATION_ASSESSMENT.md` (up to 6.61).

---

## 1. What is left

### 1.1 iOS app (Phase 5) - 4-8 weeks, excluding local AI

- **Notifications**: OGS push (1.2) replaces background polling, so no `BGAppRefreshTask` in v1.
- **Stubbed bindings**: `Analytics` and `GameNotificationCanceller` (no-ops from Swift),
  `CrashReporter` (logs only, see 1.3), `SupporterStore` (StoreKit or RevenueCat, plus iOS wording
  for the supporter copy), Google sign-in (always reports "cancelled").
- **App Store plumbing**, and a macOS stack on Bitrise.

### 1.2 Push notifications (OGS push API) - about a week, both platforms

OGS pushes its own notifications to third-party apps through FCM or APNs
(`online-go.com/docs/push-notifications.md`). This replaces polling on iOS and makes Android
notifications arrive in seconds instead of on the 15-minute WorkManager poll, with far fewer
wake-ups.

**How OGS works**

- The app is identified by an OGS OAuth application `client_id`, even though we log in with
  session cookies. The `client_id` is public and can be a constant.
- Credentials are uploaded on `online-go.com/developer` by the application's owner: the Firebase
  service account JSON (role `roles/firebasecloudmessaging.admin` only) and/or an APNs `.p8` key.
  OGS checks them with Google/Apple and stores them encrypted. The page also shows delivery
  counts, recent errors and a "Send test push" button.
- `POST /api/v1/me/push_devices` with `client_id`, `provider` (`fcm`/`apns`), `token`,
  `apns_environment` (APNs only) and `fcm_message_type` (`notification`, default, or `data`).
  Call it on every signed-in launch and on every token refresh. Registering on an install where
  another player was signed in moves the token to the new player.
- `DELETE /api/v1/me/push_devices?token=…` before logout, while the session still works. Devices
  are forgotten after 90 days without registering, or when Google/Apple report the token invalid.
- A push goes out about 10 seconds after a notification is created, if it is still unread.
  `yourMove` is only pushed while the player isn't connected to OGS, so our foreground websocket
  suppresses it. `yourMove` and `timecop` are high priority, replace the previous push for the
  same game and expire when the clock runs out. Players choose which types are pushed in OGS
  settings.
- Data keys: `type` (`yourMove`, `challenge`, `gameEnded`, `mail`, ...), `notification_id`,
  `game_id`, and `notification` (the full notification as JSON, when small enough). The title is
  OGS's localized one-line summary ("It's your move: alice vs bob").
- Pushes are not a complete record (offline devices keep only the latest), so the app still
  reconciles on open (`GET /api/v1/me/notifications`, active games, challenges).

**Beta first**: push only exists on `beta.online-go.com` until we have tested it there, and only
then goes to production. Beta has its own database and its own OAuth application registry, and
its websocket is `wss://beta.online-go.com/` (production uses `wsp.online-go.com`). So the first
step is a build-time server switch (beta is needed rarely, so no runtime selector). **Done**:

- **One place for every difference**: `OgsEnvironment` in `data/ogs` (`PRODUCTION`, `BETA`), each
  entry carrying its base URL and websocket host. The push `client_id` is added there with the
  push work, as is anything else found to differ (for example the Google sign-in server client
  ID). `OGS_ENVIRONMENT` selects one; the HTTP client, cookie store, Ktorfit, websocket and the
  game's browser/SGF links all read it. Nothing else reads a host.
- **Switching** is a local, uncommitted one-line edit of `OGS_ENVIRONMENT`, and it covers Android
  and iOS builds alike. `OgsEnvironmentTest` fails if `BETA` is ever committed.
- **Before installing a build for the other server, clear the app's data** (or log out). Cookies
  are bound to one host and beta's IDs would clash with production's cached games.
- Each server needs its own OAuth application and its own push credentials on its `/developer`
  page; the Firebase project is shared.

**Shared code**: register/unregister in `OGSRestAPI`, plus a small repository that registers on
login, launch and token change and unregisters before logout. Platforms supply only the token and
registration parameters. `UserSessionRepository.logOut()` must unregister before
`appDataEraser.clearAppData()` clears the cookies. The 401 auto-logout in `CheckNotificationsTask`
can't unregister because the session is already dead; OGS drops the device on the next
registration or after 90 days.

**Android: FCM with `fcm_message_type: "data"`**

- Our notifications are rich (board bitmap, per-speed channels, countdown, group summary,
  cancelling a stale "your move") in `NotificationUtils`. `notification` messages are drawn by the
  system while the app is in the background and would bypass all of that. `data` messages always
  reach `onMessageReceived` and are never collapsed or throttled.
- `onMessageReceived` enqueues expedited one-off work that runs the existing
  `CheckNotificationsTask`. The handler's ~10-second window is too short for a refresh plus board
  rendering. The diff against `GameDao`'s stored notifications already prevents duplicates with
  the poll.
- Keep `SynchronizeGamesWork` as a fallback and lengthen its period (1-6 hours) once push is proven.
- Needs `firebase-messaging` (the Firebase BOM is already there) and a `FirebaseMessagingService`
  in `:app`.
- Risk: FCM lowers the priority of high-priority data messages that don't lead to a visible
  notification. That only happens when our diff decides there is nothing new.

**iOS: FCM with `fcm_message_type: "notification"`** (see 1.3 for the APNs-direct alternative)

- Pushes are alerts that iOS shows itself while the app is in the background; no app code runs.
  Data-only messages would be throttled silent pushes, so iOS must not use `data`.
- Tapping a push routes `game_id` to the game screen. In the foreground,
  `userNotificationCenter(_:willPresent:)` suppresses them, matching Android.
- The text is OGS's, in the OGS account's language rather than the in-app language. Plain text in
  v1.
- Later: pushes carry `mutable-content`, so a Notification Service Extension could add a board
  image. It runs as a separate process with a tight memory limit, so it is out of scope for v1.

**Owner setup**: add the iOS app to the existing Firebase project and upload the APNs `.p8` key to
Firebase (restricted to the bundle ID). On beta now, and on production at rollout: create the OAuth
application and upload the service account JSON on that server's `/developer` page.

**To check on beta**

- The full list of `type` values.
- Status codes and bodies for the `push_devices` POST and DELETE.
- Whether the title follows the account's language.
- Nothing is pushed when it stops being our move, so a "your move" notification stays visible
  until the app reconciles.
- Registration failures (production before rollout, network errors) must be harmless; the poll
  stays as the fallback, so the push code can ship before production has the endpoint.

### 1.3 Open decisions

- **iOS crash reporting**: Crashlytics, so iOS and Android crashes are in one console. Kotlin
  exceptions need CrashKiOS (via `kermit-crashlytics`, which also turns Kermit logs into
  breadcrumbs) or they show as a bare `SIGABRT`; dSYMs must be uploaded. It fills the
  `CrashReporter` stub one-to-one. Alternatives: Sentry's KMP SDK (one SDK in common code, but a
  second console unless Android moves too), and Xcode Organizer crash reports (free, no SDK, but
  opt-in users only, delayed and without breadcrumbs).
- **iOS push transport**: recommendation FCM, because Crashlytics brings the Firebase SDK to iOS
  anyway. OGS then holds one credential (the FCM service account), the `.p8` key stays in
  Firebase, both platforms share registration with `provider: "fcm"`, and there is no
  `apns_environment` to get right per build. Costs: an extra hop through Google, Firebase-to-APNs
  misconfiguration only shows in OGS's recent errors, and FirebaseMessaging swizzles the app
  delegate (it can be turned off). If Firebase stays off iOS, use APNs directly: OGS holds the
  `.p8`, and we send `sandbox` for debug builds and `production` for TestFlight and App Store.
  Either way the `.p8` key can only send pushes (it can't sign or upload builds), can be revoked at
  any time and should be restricted to the bundle ID. A leaked key is only useful together with
  OGS's device tokens, and someone who compromises OGS can push to our users through either route.

- **Does iOS v1 ship without local AI?** KataGo runs as a child process, which iOS forbids; it
  would need relinking as an in-process library, and its memory then shares the app's jetsam
  budget. Recommendation: ship without it and hide the entry point (iOS binds
  `UnavailableAnalysisEngine` today, which shows an error state).
- **`minSdk` 26** would drop core library desugaring. It is 24 today.

### 1.4 Kept long-term

The legacy cookie import (`CookiePersistence` prefs, `SerializableCookie`): delete once Play
Console shows pre-6.5 installs are negligible, or those users get logged out.

### 1.5 On-device checks still owed

- The seven untested body-bearing endpoints: `createAccount`, `openChallenge`, `challengePlayer`,
  `markPuzzleSolved`, `ratePuzzle`, `acknowledgeWarning`, `deleteAccount`.
- Crashlytics breadcrumbs arriving as `I/Tag: message`; release logcat silent.
- The websocket on logout: `cleanup()` sent before the close frame.
- Game connection ref-counting: reopen a game; an active game keeps receiving moves after its
  screen closes; chat turns on when the screen opens.
- The review prompt after a win (blocked by Play's quota on the developer account).
- Notifications suppressed while the app is in the foreground. Once push lands, test it with OGS's
  "Send test push", with the app in the foreground, in the background and killed.

---

## 2. Architecture

```
:shared    KMP library - model, logic, network, database, data, UI
:app       Android shell - MainActivity, notifications, FCM, WorkManager, Play Billing, Play Review
iosApp/    Xcode project - UIKit shell, later push notifications, StoreKit
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
- **Push logout**: unregister the push token before logout clears the cookies, or the old account
  keeps receiving pushes on this device until another login moves the token.
- **iOS tests**: the standalone simulator test run fails every TLS handshake; real-network tests
  need a booted simulator.
- **Known bug, deliberately unfixed**: the Joseki label regex skips columns A-H with two-digit
  rows (`[A-H]|[J-T]\d{1,2}` should be `[A-HJ-T]\d{1,2}`).

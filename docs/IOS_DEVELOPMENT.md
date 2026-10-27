# iOS development, for Android developers

The iOS app lives in `iosApp/`. It is a thin native shell: all screens, logic and resources come
from `:shared`, compiled by Kotlin/Native into a framework called `Shared`. Most day-to-day work is
still Kotlin in `:shared`. Open Xcode only to run the app, read its logs, or touch the Swift shell.

## 1. One-time setup

1. Install Xcode from the App Store and open it once, so it can install its components.
2. In Xcode, open **Settings -> Components** and make sure an iOS Simulator runtime is installed.
3. Check that the command-line tools point at that Xcode:

   ```bash
   xcode-select -p
   ```

   This should print `/Applications/Xcode.app/Contents/Developer`. If it doesn't, fix it with:

   ```bash
   sudo xcode-select -s /Applications/Xcode.app
   ```
4. Gradle needs a JDK. When Xcode runs Gradle it doesn't load your shell profile, so the build
   script falls back to Android Studio's bundled JBR if `JAVA_HOME` is unset.

## 2. Android to iOS vocabulary

| Android                           | iOS / Xcode                                                                         |
|-----------------------------------|-------------------------------------------------------------------------------------|
| Gradle project + `:app` module    | `iosApp/iosApp.xcodeproj` with one **target**, `iosApp`                             |
| Run configuration                 | **Scheme** (`iosApp`, picked in the toolbar)                                        |
| `AndroidManifest.xml`             | `iosApp/Info.plist`                                                                 |
| `applicationId`                   | Bundle identifier, `PRODUCT_BUNDLE_IDENTIFIER` (`io.zenandroid.onlinego`)           |
| `versionCode` / `versionName`     | `CURRENT_PROJECT_VERSION` / `MARKETING_VERSION` (target's Build Settings)           |
| `Application` + `MainActivity`    | `AppDelegate.swift` + `SceneDelegate.swift`                                         |
| `setContent { }` in an `Activity` | `MainViewController(versionCode:)`, a `UIViewController` hosting Compose            |
| Emulator                          | **Simulator**. It runs native arm64 code, so it's fast, but it is not a real device |
| `adb`                             | `xcrun simctl`                                                                      |
| logcat                            | Xcode's debug console, or Console.app                                               |
| `build/`                          | DerivedData (`~/Library/Developer/Xcode/DerivedData`)                               |
| "Clear data" / uninstall          | Long-press the app icon -> Remove App, or Device -> Erase All Content and Settings  |

## 3. How a build works

When you press Run, Xcode:

1. Runs the **Compile Kotlin Framework** build phase, which calls
   `./gradlew :shared:embedAndSignAppleFrameworkForXcode`. Gradle builds `:shared` for the
   architecture and configuration Xcode asked for (Debug/Release, Simulator/device) into
   `shared/build/xcode-frameworks/`. The Compose plugin also copies the Compose resources
   (strings, drawables, `stone.wav`) into the app bundle.
2. Compiles the Swift files in `iosApp/iosApp/`, which `import Shared`.
3. Links them against `Shared.framework` (a static framework) and `libc++` for the score
   estimator.
4. Copies resources: `tutorials.json` is referenced straight from `app/src/main/assets/`, so both
   apps share one file.

The first build compiles all of `:shared` with Kotlin/Native and takes several minutes. Later
builds are incremental. A Kotlin change shows up on the next Run, with no extra step.

The project uses folder-synchronised groups: any file you add under `iosApp/iosApp/` is part of
the target automatically, with no need to add it in Xcode.

## 4. Running in Xcode

```bash
open iosApp/iosApp.xcodeproj
```

1. In the toolbar at the top centre, pick the **iosApp** scheme and a device, for example
   *iPhone 17*.
2. **Cmd-R** builds, installs and launches. **Cmd-.** stops. **Cmd-B** only builds.
   **Shift-Cmd-K** cleans (like `./gradlew clean`, but only for the Swift side).
3. Logs appear in the debug console at the bottom (**Shift-Cmd-Y** toggles it). Kermit logs go to
   the unified log, so they show up there and in Console.app.
4. If the build fails, the **Report navigator** (**Cmd-9**) has the full log. A Gradle or Kotlin
   error is inside the *Compile Kotlin Framework* step.

You can't step through Kotlin code in Xcode out of the box. Use Kermit logs; for breakpoints in
Kotlin, run the iOS app from Android Studio or IntelliJ with the Kotlin Multiplatform plugin, or
install Touchlab's `xcode-kotlin` plugin into Xcode.

## 5. The Simulator

| Want to                      | Do                                                                                                                                |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Rotate                       | **Cmd-Left** / **Cmd-Right**                                                                                                      |
| Go home (background the app) | **Shift-Cmd-H**                                                                                                                   |
| Toggle dark mode             | Features -> Toggle Appearance (**Shift-Cmd-A**)                                                                                   |
| Type with the Mac keyboard   | I/O -> Keyboard -> Connect Hardware Keyboard (**Shift-Cmd-K**)                                                                    |
| Show the on-screen keyboard  | **Cmd-K**                                                                                                                         |
| Change language              | Settings app inside the Simulator -> General -> Language & Region, or the app's own Language setting (applies on the next launch) |
| Wipe all data                | Device -> Erase All Content and Settings                                                                                          |
| Screenshot                   | **Cmd-S** (saved to the Desktop)                                                                                                  |

The same from the command line:

```bash
xcrun simctl list devices available
```

```bash
xcrun simctl boot "iPhone 17"
```

```bash
xcrun simctl ui booted appearance dark
```

```bash
xcrun simctl uninstall booted io.zenandroid.onlinego
```

## 6. Building without opening Xcode

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/ios build
```

```bash
xcrun simctl install booted build/ios/Build/Products/Debug-iphonesimulator/OnlineGo.app
```

```bash
xcrun simctl launch --console booted io.zenandroid.onlinego
```

`--console` streams the app's output to the terminal, like `adb logcat` filtered to one app.

## 7. Running on a real iPhone

1. Plug the phone in. On the phone, accept "Trust this computer" and turn on **Developer Mode**
   (Settings -> Privacy & Security).
2. In Xcode, select the `iosApp` target -> **Signing & Capabilities** and pick your team. A free
   Apple ID works, but the app expires after 7 days and the bundle identifier may need a suffix.
3. Pick the phone as the run destination and press Cmd-R.

## 8. Calling Kotlin from Swift

The framework header (`shared/build/bin/.../Shared.framework/Headers/Shared.h`) shows exactly
what Swift sees. The rules you'll hit:

- A top-level function in `Foo.kt` is a static method on `FooKt`:
  `MainViewControllerKt.MainViewController(versionCode:)`.
  A file named `Prewarm.ios.kt` becomes `Prewarm_iosKt`.
- Names starting with `init` get a `do` prefix: `initKoin` is `InitKoinKt.doInitKoin(...)`.
- Kotlin interfaces become Swift protocols, which Swift classes implement (see
  `PlatformBindings.swift`).
- `Int` is `Int32`, `Long` is `Int64`, `Map<String, String?>` is `[String: Any]`.
- `suspend` functions become Swift `async` functions.

## 9. What the shell doesn't do yet

These are still stubs, listed in `docs/KMP_MIGRATION_ASSESSMENT.md`:

- `AppDataEraser` is a no-op, **so logging out doesn't clear the session**. It has to clear the
  database, the DataStores and both `NSUserDefaults` suites.
- Analytics, crash reporting, in-app review, notifications and background refresh.
- Google sign-in reports "cancelled", and the Supporter screen has no store behind it.
- HTML text shows line breaks only, and avatars don't load (Coil has no network fetcher on iOS
  yet).
- Local AI: the AI game screen shows an error.
- There is no app icon yet.

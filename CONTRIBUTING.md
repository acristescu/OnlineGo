Contributing
===

General Notes on contributing to the project

Getting Started
---

You will need to create a Google FireBase Project and generate a `google-services.json` file to successfully build the project.

1. Login to [console.firebase.google.com](https://console.firebase.google.com)
2. Create a Project with any name (doesn't matter) what it is
3. Uncheck the sharing settings for analytics data and create the project. <br>

   You should now be at the Project page in the Firebase Console

4. Press Android icon
5. Press `Register App` after setting the following:

   `Package Name: io.zenandroid.onlinego.debug`
   > It's the `applicationId` found in `app/build.gradle`

   `App Nickname: anything you want`
   > It is just a user friendly way to identify it in the Firebase Console

   Omit the debug signing cert

6. Download the `google-services.json` file and put it in the `app/` folder in Android Studio

You can now [build/run the project](https://developer.android.com/studio/run).

Troubleshooting
---

When syncing gradle, the following issues may arise.

- Error: `The project is using an incompatible version (AGP x.x.x-alpha) of the Android Gradle plugin. Latest supported version is AGP x.x.x`
    - This repo tends to use a newer version of the Android Gradle Plugin.  You can try either of the following solutions.
    - Solution: Downgrade the AGP version specified in [build.gradle](https://github.com/acristescu/OnlineGo/blob/c7d8c00da95424ec5390bddcdd0a10319c4a0f77/build.gradle#L12)
    - Solution: install the [Android Studio Canary version](https://developer.android.com/studio/preview)

- Error: `CMake '3.10.2' was not found in SDK, PATH, or by cmake.dir property.`
    - Solution: [Install CMake](https://developer.android.com/studio/projects/install-ndk#default-version)

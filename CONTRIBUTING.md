Contributing
===
> General Notes on contributing to the project
Getting Started
---

You will need to create a Google FireBase Project and generate a `google-services.json` file to successfully build the project.

1. Login to [console.firebase.google.com](console.firebase.com)
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

You can now build/run the project.
# Localazy

Localazy is a continuous localization platform and web-based translation management system (TMS)
In an effort to make Go accessible worldwide there is an ongoing translation of the app.
Feel free to submit some translation ! [contribute to translation](https://localazy.com/p/onlinego)

## How to use it for devs
Localazy gradle plugin allows for uploading new string resources to the localazy website in order to make them available for translation contribution.
It also download those translations back from the website, creating localization files during build (no change to sourcecode).

### build.gradle(:app)
Uncomment localazy config in 'build.gradle'
Contact devs for secret read and write keys.

### res>values>strings
We only use a single strings.xml file, there is no need for localized files in the source code, they are created automatically during the build.
You can find all the translated strings.xml files in the build directory after a successful build.

### uploadStrings
When used for the first time / in a new project, upload strings in all languages for translation.
Subsequent uses upload only changes to the base language as all future changes to strings should be made only to the base language.
This task uses hidden .localazy file in the project directory to store state information.

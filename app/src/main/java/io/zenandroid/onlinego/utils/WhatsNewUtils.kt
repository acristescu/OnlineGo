package io.zenandroid.onlinego.utils
import java.security.MessageDigest

object WhatsNewUtils {

    val shouldDisplayDialog: Boolean
        get() = PersistenceManager.previousWhatsNewTextHashed != null && PersistenceManager.previousWhatsNewTextHashed != hashString(currentText)
    val whatsNewText = currentText

    fun textShown() {
        PersistenceManager.previousWhatsNewTextHashed = hashString(currentText)
    }

    private fun hashString(text: String): String {
        return MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).fold("", { str, it -> str + "%02x".format(it) })
    }
}

private const val currentText = """
## What's new

- Shiny new app icon.
- I rewrote the settings screen so that it looks cleaner and helps people find the desired settings easier.
- Added the ability to become a project supporter by doing a monthly donation though Google Play. There are no supporter-only features, this is meant as a donation for the project. 
- Fixed a bug that prevented old low-memory devices from using google login
- Changed a few of the icons to make them more consistent. Yes, I did have to learn how to create vector icons using Figma...
- Pressing back after navigating to several games now takes you back directly to the home screen (thanks to sdeframond for the contribution)

## About project

This is an open-source project. To find out how you can contribute or support the project, head over to the project’s [Github page](https://github.com/acristescu/OnlineGo).
"""
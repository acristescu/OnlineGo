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

- Fixed a bug that prevented the game notification button (top right on most screens) from cycling through available games
- Fixed a bug that caused the notification button's badge to flicker while changing screens
- Fixed a bug on high DPI devices where the tutorials could not be clicked correctly

## About project

This is an open-source project. To find out how you can contribute or support the project, head over to the project’s [Github page](https://github.com/acristescu/OnlineGo). If you'd like to financially support the project instead, please visit the Support page (in the settings).
"""
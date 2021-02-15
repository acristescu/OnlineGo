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

- Made the dark mode darker, as per your feedback
- Reworked the toolbar in order to give it a more modern look (more to come)
- Fixed a bug where the border of the game thumbnails was too large on some devices
- Improved the landscape mode layouts to give more space to the game board
- Made the bottom sheet dialogs appear expanded even in landscape mode

## About project

This is an open-source project. To find out how you can contribute or support the project, head over to the project’s [Github page](https://github.com/acristescu/OnlineGo). If you'd like to financially support the project instead, please visit the Support page (in the settings).
"""
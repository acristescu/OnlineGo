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

- Games that are paused (for example during the weekend or for a vacation) are now marked as such in the main screen
- Fixed a few rare crashes that happened when going out of the app with some dialogs shown

## Last version:

- Fixed a bug that prevented the game notification button (top right on most screens) from cycling through available games
- Fixed a bug that caused the AI to get stuck until you kill the app once you navigate away from an AI game
- Fixed a bug that caused the notification button's badge to flicker while changing screens
- Fixed a bug on high DPI devices where the tutorials could not be clicked correctly
- Faster app startup (no more blue background screen flickering)
- Implemented a new login system and onboarding screen. You can give it a spin by just logging out if you're curious. Just make sure you know your password :)
- Made all chats that are older than your most recent chat be considered "read". This should make it easier for people that are using both the website AND the app to chat.

## About project

This is an open-source project. To find out how you can contribute or support the project, head over to the project’s [Github page](https://github.com/acristescu/OnlineGo). If you'd like to financially support the project instead, please visit the Support page (in the settings).
"""
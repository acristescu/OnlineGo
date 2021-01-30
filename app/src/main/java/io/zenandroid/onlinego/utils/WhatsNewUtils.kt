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

- New tutorial based on the now defunct "Interactive way to go" (with permission). It's incomplete right now, but there is plenty to send feedback about
- Fixed back button navigation once and for all (hopefully). For those that care, we're now using Jetpack Navigation under the hood
- Reimplemented the learn page as it was looking a bit dated
- Fixed dark mode icon issues
- Made dark mode colors more consistent between screens
- Fixed launcher icon color to always be blue (there was a bug in which it appeared salmon in light mode and blue in dark mode)
- Changed the stones images to look a bit sharper, particularly on tablets
- Changed the board background in both light and dark themes
- Changed the stones shadow to make them "pop" a little more
- Made the board previews one the main page flatter. Should look better and be a lot faster (only noticeable on old devices)
- Introduced a subtle animation for the board (only in tutorials for now) when stones are captured
- Adjusted the displayed time of the games to show a bit more detail (e.g. 1d 23h instead of 1 day) - Thanks to v011 for the contribution)
- Changed the rank formula to match the new OGS changes (thanks to Popz for the contribution)
- (under the hood) brought in some bleeding edge libraries such as Jetpack Compose. For now these are mostly on the Learn and Tutorial screens, but more will come in the future, so please report any new issues (crashes, slowness, weird board states)

## About project

This is an open-source project. To find out how you can contribute or support the project, head over to the project’s [Github page](https://github.com/acristescu/OnlineGo). If you'd like to financially support the project instead, please visit the Support page (in the settings).
"""
package io.zenandroid.onlinego.utils
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

object WhatsNewUtils {

    val shouldDisplayDialog: Boolean
        get() = PersistenceManager.previousWhatsNewTextHashed != null && PersistenceManager.previousWhatsNewTextHashed != hashString(annotatedCurrentText.text)
    val whatsNewTextAnnotated = annotatedCurrentText

    fun textShown() {
        PersistenceManager.previousWhatsNewTextHashed = hashString(annotatedCurrentText.text)
    }

    private fun hashString(text: String): String {
        return MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).fold("", { str, it -> str + "%02x".format(it) })
    }
}

private val annotatedCurrentText = AnnotatedString.Builder().run {
    pushStyle(SpanStyle(fontSize = 18.sp))
    append("What's new\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("· Fixed a bug in which looking for a game (aka AutoMatch) would not show up on the main screen.\n\n")
    append("· Added a countdown timer on top of the last move marker when playing live and you're in the last 10 seconds of your time (or byo-yomi period).\n\n")
    append("· Fixed a bug in which the app behaved weirdly after logging out. Logging out now forcefully restarts the app.\n\n")
    append("· Disabled Facebook log in as it stopped working for Android 12 and up.\n\n")
    append("· Added a couple more stats to the game's player details dialog. One of them is the percentile, which means the percentage of OGS users who are rated lower than you. More to come.\n\n")
    append("· Fixed some issues with Analysis mode.\n\n")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("Previous version\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("· Rewrote the entire game screen to use modern technologies (Jetpack Compose and the Molecule library). This should make any further development a lot easier, but will likely introduce a lot of bugs initially. Please report any bugs you find.\n\n")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("About project\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("This is an open-source project. If you want to contribute, the code is available on Github. If you'd like to financially support the project instead, please visit the Support page.")
    toAnnotatedString()
}
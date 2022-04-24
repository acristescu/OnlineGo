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
    append("· Bugfixes\n\n")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("Previous version\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("· You can now use analysis on games that are finished even if analysis was disabled.\n\n")
    append("· Fixed a bug that misinterpreted KO during analysis.\n\n")
    append("· Fixed some problems with the Joseki Explorer.\n\n")
    append("· Fixed some problems scoring under Chinese rules.\n\n")
    append("· You can now start analyzing (trying out what-if scenarios) from the history of the game, not just the last move.\n\n")
    append("· Rewrote the game logic to optimize for memory usage. This should help users with old devices and/or many games. Please report any issues you may encounter.\n\n")
    append("· Fixed some AI game bugs.\n\n")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("About project\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("This is an open-source project. If you want to contribute, the code is available on Github. If you'd like to financially support the project instead, please visit the Support page.")
    toAnnotatedString()
}
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
    append("· Non square games are now supported (if started from the website).\n\n")
    append("· Your turn games are sorted by time left.\n\n")
    append("· Speed and memory improvements for people with lots of games.\n\n")
    append("· Bugfixes.\n\n")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("Previous version\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("· If it's your turn in more than 10 games, we show a list of them instead of using a carousel.\n\n")
    append("· Some more bug fixes for people with a lot of games.\n\n")
    append("· Game handicap is now visible in Game Info\n\n")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("About project\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("This is an open-source project. If you want to contribute, the code is available on Github. If you'd like to financially support the project instead, please visit the Support page.")
    toAnnotatedString()
}
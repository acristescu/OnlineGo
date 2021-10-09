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
    append("· Hotfix as the last version broke notifications on Android 12. OOPS!\n\n")
    pop()
    pushStyle(SpanStyle(fontSize = 18.sp))
    append("Previous version\n\n")
    pop()
    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("· Bug fixes for people playing MAAANY games (800+)\n\n")
    append("· Bug fix that caused some user to not be able to return to the main screen after playing a live game\n\n")
    append("· More UI redesign (moving towards Material You)\n\n")
    pop()
    pushStyle(SpanStyle(fontSize = 18.sp))
    append("About project\n\n")
    pop()
    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("This is an open-source project. If you want to contribute, the code is available on Github. If you'd like to financially support the project instead, please visit the Support page.")
    toAnnotatedString()
}
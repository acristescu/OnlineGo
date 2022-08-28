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
    append("· Implemented a new chart in the stats page. Don't forget to tap on it to see details!\n\n")
    append("· Fixed a bug with the scoring of AI games. Thanks to @ArsenLapin1 for pointing that out.\n\n")
    append("· Fixed a few crashes.\n\n")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("Previous version\n\n")
    append("· Added support for Android 13 themed icons.\n\n")
    append("· Added support for Android 13 notification request. Thanks to Fabien for his help!\n\n")
    append("· Fixed a crash related to stats for users that have no games completed.\n\n")
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
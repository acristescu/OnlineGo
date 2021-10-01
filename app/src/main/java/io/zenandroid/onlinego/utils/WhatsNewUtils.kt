package io.zenandroid.onlinego.utils
import android.graphics.fonts.FontStyle
import androidx.compose.material.MaterialTheme
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
    append("· Updated the look of the home screen (for those interested, it's using the new Jetpack Compose technology)\n\n")
    append("· Games that are paused (for example during the weekend or for a vacation) are now marked as such in the main screen\n\n")
    append("· Fixed issues with dark theme during the onboarding experience\n\n")
    append("· Fixed a bug where the user could get stuck in a loop if Google Sign in failed\n\n")
    append("· Fixed a few rare crashes that happened when going out of the app with some dialogs shown\n\n")
    append("· Fixed a problem that made the app not connect to the server immediately after loggin in\n\n")
    append("· Added more tutorials\n\n")
    pop()
    pushStyle(SpanStyle(fontSize = 18.sp))
    append("About project\n\n")
    pop()
    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("This is an open-source project. If you want to contribute, the code is available on Github. If you'd like to financially support the project instead, please visit the Support page.")
    toAnnotatedString()
}
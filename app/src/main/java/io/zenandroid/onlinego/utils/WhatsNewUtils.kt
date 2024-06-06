package io.zenandroid.onlinego.utils
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
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
    append("· Hot fix for some notification issues that were introduced in the previous version.")
    append("\n")
    append("· Puzzles screen (thanks to bqv for the contribution)")
    append("\n")
    append("· More notifications controls. You can now disable notifications for live games for example.")
    append("\n")
    append("· Screen will now stay on while playing a game (thanks to kpe for the contribution)")
    append("\n")
    append("· Stats screen improvements (thanks to bqv for the contribution)")
    append("\n")
    append("· Fixed issue with rewinding a face-to-face game with handicap")
    append("\n")
    append("· Fixed an issue with refusing to give notification permissions causing the app to become unresponsive.")
    append("\n")
    append("· Lots of small bugfixes")
    pop()

    pushStyle(SpanStyle(fontSize = 18.sp))
    append("\n\n")
    append("About project\n\n")
    pop()

    pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
    append("This is an open-source project. If you want to contribute, the code is available on Github.")
    toAnnotatedString()
}

@Preview
@Composable
fun Preview() {
    OnlineGoTheme {
        AlertDialog(onDismissRequest = { /*TODO*/ },
            dismissButton = {
                TextButton(onClick = {}) { Text("OK") }
            },
            confirmButton = {
                TextButton(onClick = { }) { Text("SUPPORT") }
            },
            text = {
                Text(text = WhatsNewUtils.whatsNewTextAnnotated)
            }
        )
    }
}
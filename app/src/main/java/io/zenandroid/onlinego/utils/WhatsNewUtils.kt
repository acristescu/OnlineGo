package io.zenandroid.onlinego.utils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    append("· Updated the 'Play online' to match the new OGS website design\n")
    append("\n")
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
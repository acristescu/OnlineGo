package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private const val WHATS_NEW = "WHATS_NEW"
private val WHATS_NEW_KEY = stringPreferencesKey(WHATS_NEW)
val Context.whatsNewDataStore by preferencesDataStore(name = "whats_new")

object WhatsNewUtils {
  suspend fun shouldDisplayDialog(context: Context): Boolean {
    return withContext(Dispatchers.IO) {
      val hash = hashString(annotatedCurrentText.text)
      val stored = context.whatsNewDataStore.data.map { it[WHATS_NEW_KEY] }.first()
      stored != null && stored != hash
    }
  }

  suspend fun textShown(context: Context) {
    withContext(Dispatchers.IO) {
      val hash = hashString(annotatedCurrentText.text)
      context.whatsNewDataStore.edit { prefs ->
        prefs[WHATS_NEW_KEY] = hash
      }
    }
  }

  val whatsNewTextAnnotated = annotatedCurrentText

  private fun hashString(text: String): String {
    return MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
      .fold("", { str, it -> str + "%02x".format(it) })
  }
}

private val annotatedCurrentText = AnnotatedString.Builder().run {
  pushStyle(SpanStyle(fontSize = 20.sp))
  append("What's new\n\n")
  pop()

  pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
  append("· New Look and Feel with colors chosen from your Android wallpaper\n")
  append("· OGS Moderator warnings are now displayed\n")
  append("· Prepared app for Android 15\n")
  append("· Reimplemented Face To Face, AI and Joseki screens\n")
  append("· Faster startup\n")
  append("· Migrated navigation to compose\n")
  pop()

  pushStyle(SpanStyle(fontSize = 20.sp))
  append("\n")
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
    AlertDialog(
      onDismissRequest = {},
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
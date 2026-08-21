package io.zenandroid.onlinego.utils

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.zenandroid.onlinego.R
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
      val hash = hashString(whatsNewText(context).text)
      val stored = context.whatsNewDataStore.data.map { it[WHATS_NEW_KEY] }.first()
      stored != null && stored != hash
    }
  }

  suspend fun textShown(context: Context) {
    withContext(Dispatchers.IO) {
      val hash = hashString(whatsNewText(context).text)
      context.whatsNewDataStore.edit { prefs ->
        prefs[WHATS_NEW_KEY] = hash
      }
    }
  }

  private fun hashString(text: String): String {
    return MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
      .fold("", { str, it -> str + "%02x".format(it) })
  }
}

fun whatsNewText(context: Context): AnnotatedString = AnnotatedString.Builder().run {
  pushStyle(SpanStyle(fontSize = 20.sp))
  append("${context.getString(R.string.whats_new_title)}\n\n")
  pop()

  pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
  append(context.getString(R.string.whats_new_changelog))
  pop()

  pushStyle(SpanStyle(fontSize = 20.sp))
  append("\n")
  append("${context.getString(R.string.whats_new_about_title)}\n\n")
  pop()

  pushStyle(SpanStyle(fontWeight = FontWeight.Normal))
  append(context.getString(R.string.whats_new_about_body))
  toAnnotatedString()
}

@Preview
@Composable
fun Preview() {
  OnlineGoTheme {
    AlertDialog(
      onDismissRequest = {},
      dismissButton = {
        TextButton(onClick = {}) { Text(stringResource(R.string.ok)) }
      },
      confirmButton = {
        TextButton(onClick = { }) { Text(stringResource(R.string.mygames_support)) }
      },
      text = {
        Text(text = whatsNewText(LocalContext.current))
      }
    )
  }
}
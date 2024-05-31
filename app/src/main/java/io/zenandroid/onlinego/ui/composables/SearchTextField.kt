package io.zenandroid.onlinego.ui.composables

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SearchTextField(
  value: String,
  onValueChange: (String) -> Unit,
  hint: String,
  modifier: Modifier = Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions(),
  onCleared: (() -> Unit) = { onValueChange("") },
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null, // decorative
      )
    },
    trailingIcon = {
      IconButton(
        onClick = onCleared,
      ) {
        Icon(
          imageVector = Icons.Default.Clear,
          contentDescription = "Clear text"
        )
      }
    },
    placeholder = { Text(text = hint) },
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    maxLines = 1,
    singleLine = true,
//    colors = TextFieldDefaults.colors(
//      focusedContainerColor = MaterialTheme.colors.surface,
//      focusedTextColor = MaterialTheme.colors.onSurface
//    ),
    modifier = modifier,
  )
}
package io.zenandroid.onlinego.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons.Rounded
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded._123
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R.drawable
import io.zenandroid.onlinego.R.mipmap
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.ui.screens.mygames.composables.SenteCard
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.BoardThemeClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.CoordinatesClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DeleteAccountCanceled
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DeleteAccountClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.DeleteAccountConfirmed
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.LogoutClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.NotificationsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.PrivacyClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.RanksClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SoundsClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.SupportClicked
import io.zenandroid.onlinego.ui.screens.settings.SettingsAction.ThemeClicked
import io.zenandroid.onlinego.ui.theme.OnlineGoTheme
import io.zenandroid.onlinego.utils.processGravatarURL
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel = koinViewModel(),
  onNavigateToSupport: () -> Unit,
) {
  val state by viewModel.state.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()

  var dialogData by remember { mutableStateOf<DialogData?>(null) }

  val activity = LocalActivity.current

  SettingsContent(
    state = state,
    userSettings = userSettings,
    onAction = {
      when (it) {
        is NotificationsClicked -> navigateToNotifications(activity)
        is PrivacyClicked -> activity?.startActivity(
          Intent(
            Intent.ACTION_VIEW,
            "https://www.freeprivacypolicy.com/privacy/view/40f932df1dfc9af1b04df367aa6f14f0".toUri()
          )
        )

        is LogoutClicked -> dialogData = DialogData(
          title = "Log out",
          message = "Are you sure you want to log out? You won't be able to use the app until you log back in.",
          positiveButton = "Log out",
          negativeButton = "Cancel",
          onPositive = { viewModel.onAction(SettingsAction.Logout(activity)) },
        )

        is SupportClicked -> onNavigateToSupport()

        else -> viewModel.onAction(it)
      }
    })

  dialogData?.let { data ->
    AlertDialog(
      onDismissRequest = { dialogData = null },
      confirmButton = {
        TextButton(onClick = { data.onPositive() }) {
          Text(data.positiveButton)
        }
      },
      dismissButton = {
        TextButton(onClick = { dialogData = null }) {
          Text(data.negativeButton)
        }
      },
      text = { Text(data.message) },
      title = { Text(data.title, style = MaterialTheme.typography.titleLarge) },
    )
  }

  if (state.passwordDialogVisible) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { viewModel.onAction(DeleteAccountCanceled) },
      confirmButton = {
        TextButton(onClick = {
          if (password.isNotBlank()) {
            viewModel.onAction(
              DeleteAccountConfirmed(password)
            )
          }
        }) {
          Text("DELETE ACCOUNT")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.onAction(DeleteAccountCanceled) }) {
          Text("CANCEL")
        }
      },
      text = {
        Column {
          Text("Please enter your password to confirm account deletion")
          Spacer(modifier = Modifier.height(8.dp))

          var passwordVisibility by remember { mutableStateOf(false) }
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (!passwordVisibility) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                Icon(
                  imageVector = if (passwordVisibility) Filled.Visibility else Filled.VisibilityOff,
                  contentDescription = ""
                )
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          )
        }
      },
      title = { Text("Delete Account") },
    )
  }

  if (state.modalVisible) {
    Dialog(
      onDismissRequest = { },
      DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
      Box(
        contentAlignment = Alignment.Center, modifier = Modifier
          .size(100.dp)
          .background(
            color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)
          )
      ) {
        CircularProgressIndicator()
      }
    }
  }

  state.deleteAccountError?.let {
    AlertDialog(
      onDismissRequest = { viewModel.onAction(DeleteAccountCanceled) },
      confirmButton = {
        TextButton(onClick = { viewModel.onAction(DeleteAccountCanceled) }) {
          Text("OK")
        }
      },
      text = { Text(it) },
      title = { Text("Error") },
    )
  }

}

@Composable
private fun SettingsContent(
  state: SettingsState,
  userSettings: UserSettings,
  onAction: (SettingsAction) -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .padding(top = 36.dp)
        .size(84.dp)
        .aspectRatio(1f, true)
    ) {
      val shape = RoundedCornerShape(14.dp)
      Image(
        painter = rememberAsyncImagePainter(
          model = ImageRequest.Builder(LocalContext.current).data(
            processGravatarURL(
              state.avatarURL, LocalDensity.current.run { 84.dp.toPx().toInt() })
          ).placeholder(mipmap.placeholder).error(mipmap.placeholder).build()
        ),
        contentDescription = "Avatar",
        modifier = Modifier
          .size(84.dp)
          .fillMaxSize()
          .padding(bottom = 4.dp, end = 4.dp)
          .shadow(2.dp, shape)
          .clip(shape)
      )
    }

    Text(
      text = state.username,
      fontWeight = FontWeight.Medium,
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(top = 12.dp)
    )
    Button(
      onClick = { onAction(SupportClicked) },
      shape = RoundedCornerShape(14.dp),
      elevation = ButtonDefaults.elevatedButtonElevation(
        defaultElevation = 4.dp,
        pressedElevation = 0.dp,
      ),
      modifier = Modifier
        .padding(top = 24.dp)
        .height(36.dp)
    ) {
      Icon(
        imageVector = ImageVector.vectorResource(drawable.ic_star),
        contentDescription = "Become a supporter",
        modifier = Modifier.padding(end = 12.dp, start = 8.dp)
      )
      Text(
        text = "Become a supporter",
        modifier = Modifier.padding(end = 8.dp),
        fontWeight = FontWeight.Medium,
      )
    }
    Section(title = "Notifications") {
      Column(modifier = Modifier) {
        SettingsRow(
          title = "System notifications",
          icon = Filled.Notifications,
          checkbox = false,
          checked = false,
          onClick = { onAction(NotificationsClicked) })
        SettingsRow(
          title = "Stone Sounds",
          icon = Icons.AutoMirrored.Filled.VolumeUp,
          checkbox = true,
          checked = userSettings.soundEnabled,
          onClick = { onAction(SoundsClicked) })
      }
    }
    Section(title = "Appearance") {
      Column(modifier = Modifier) {
        SettingsRow(
          title = "Theme",
          icon = Rounded.DarkMode,
          checkbox = false,
          checked = true,
          value = userSettings.theme,
          possibleValues = listOf(
            "System Default", "Light", "Dark"
          ),
          onValueClick = { onAction(ThemeClicked(it)) })
        SettingsRow(
          title = "Board style",
          icon = Rounded.Palette,
          checkbox = false,
          checked = true,
          value = userSettings.boardTheme.displayName,
          possibleValues = BoardTheme.entries,
          onValueClick = { onAction(BoardThemeClicked(it)) })
      }
    }
    Section(title = "Game Settings") {
      Column(modifier = Modifier) {
        SettingsRow(
          title = "Show Coordinates",
          icon = Rounded._123,
          checkbox = true,
          checked = userSettings.showCoordinates,
          onClick = { onAction(CoordinatesClicked) })
        SettingsRow(
          title = "Show Player Ranks",
          icon = Rounded.MilitaryTech,
          checkbox = true,
          checked = userSettings.showRanks,
          onClick = { onAction(RanksClicked) })
      }
    }
    Section(title = "Account") {
      Column(modifier = Modifier) {
        SettingsRow(
          title = "Logout",
          icon = Icons.AutoMirrored.Rounded.Logout,
          checkbox = false,
          checked = true,
          onClick = { onAction(LogoutClicked) })
        SettingsRow(
          title = "Delete account",
          icon = Rounded.HeartBroken,
          checkbox = false,
          checked = true,
          onClick = { onAction(DeleteAccountClicked) })
      }
    }
    Section(title = "Legal") {
      Column(modifier = Modifier) {
        SettingsRow(
          title = "Privacy Policy",
          icon = Filled.Lock,
          checkbox = false,
          checked = true,
          onClick = { onAction(PrivacyClicked) })
      }
    }
    Text(
      text = "Build no. ${BuildConfig.VERSION_CODE}",
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(vertical = 32.dp),
    )
  }
}

@Composable
private fun SettingsRow(
  title: String,
  icon: ImageVector,
  checkbox: Boolean = false,
  checked: Boolean = false,
  value: String? = null,
  possibleValues: List<Any> = emptyList(),
  onClick: () -> Unit = {},
  onValueClick: (String) -> Unit = {},
) {
  var menuOpen by remember { mutableStateOf(false) }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .height(64.dp)
      .fillMaxWidth()
      .clickable {
        if (possibleValues.isNotEmpty()) {
          menuOpen = true
        } else {
          onClick()
        }
      }) {
    Icon(
      imageVector = icon,
      contentDescription = "Icon",
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 24.dp)
    )
    Text(
      text = title,
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.weight(1f),
    )
    if (checkbox) {
      Switch(
        checked = checked, onCheckedChange = { onClick() }, modifier = Modifier.padding(end = 12.dp)
      )
    } else if (value != null) {
      Box {
        Text(
          text = value, fontSize = 14.sp, style = TextStyle(
            fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.4.sp
          ), modifier = Modifier.padding(end = 16.dp, bottom = 16.dp, top = 16.dp)
        )
        if (possibleValues.isNotEmpty()) {
          DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
          ) {
            possibleValues.forEach {
              key(it) {
                DropdownMenuItem(
                  onClick = {
                    menuOpen = false
                    onValueClick(it.toString())
                  },
                  leadingIcon = {
                    if (it is BoardTheme) {
                      Box(modifier = Modifier.size(24.dp)) {
                        if (it.backgroundImage != null) {
                          Image(
                            painter = painterResource(id = it.backgroundImage),
                            contentDescription = "Icon",
                            modifier = Modifier.size(24.dp)
                          )
                        } else {
                          Image(
                            painter = ColorPainter(colorResource(it.backgroundColor!!)),
                            contentDescription = "Icon",
                            modifier = Modifier.size(24.dp)
                          )
                        }
                        Image(
                          painter = painterResource(id = it.gridPreview),
                          contentDescription = "Icon",
                          modifier = Modifier.size(24.dp)
                        )
                      }
                    }
                  },
                  text = {
                    Text(
                      text = it.toString(),
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(start = 8.dp)
                    )
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
  Text(
    text = title,
    fontSize = 12.sp,
    color = MaterialTheme.colorScheme.onBackground,
    fontWeight = FontWeight.Medium,
    modifier = Modifier
      .padding(start = 12.dp, top = 24.dp, bottom = 4.dp)
      .fillMaxWidth()
  )
  SenteCard(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 4.dp),
  ) {
    content()
  }
}

private fun navigateToNotifications(activity: Activity?) {
  val intent = Intent()
  intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"

  //for Android 5-7
  intent.putExtra("app_package", activity?.packageName)
  intent.putExtra("app_uid", activity?.applicationInfo?.uid)

  // for Android O
  intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)

  activity?.startActivity(intent)
}

@Composable
@Preview(showBackground = true)
private fun SettingsScreenPreview() {
  OnlineGoTheme {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
      SettingsContent(
        SettingsState().copy(
          username = "Username",
        ),
        UserSettings(
          theme = "System Default",
          boardTheme = BoardTheme.WOOD,
          soundEnabled = true,
          showRanks = true,
          showCoordinates = true
        ),
        {})
    }
  }
}
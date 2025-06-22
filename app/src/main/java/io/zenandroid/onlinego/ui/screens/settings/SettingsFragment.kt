package io.zenandroid.onlinego.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons.Rounded
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded._123
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.platform.ComposeView
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
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.R.drawable
import io.zenandroid.onlinego.R.mipmap
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.main.MainActivity
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
import io.zenandroid.onlinego.utils.rememberStateWithLifecycle
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

@Immutable
data class DialogData(
  val title: String,
  val message: String,
  val positiveButton: String,
  val negativeButton: String,
  val onPositive: () -> Unit,
)

class SettingsFragment : Fragment() {

  private val viewModel: SettingsViewModel by viewModel()
  private val userSessionRepository: UserSessionRepository by inject()

  private val logoutDialogData = DialogData(
    title = "Log out",
    message = "Are you sure you want to log out? You won't be able to use the app until you log back in.",
    positiveButton = "Log out",
    negativeButton = "Cancel",
    onPositive = { doLogout() },
  )

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        OnlineGoTheme {
          val state by rememberStateWithLifecycle(viewModel.state)

          var dialogData by remember { mutableStateOf<DialogData?>(null) }

          SettingsScreen(state) {
            when (it) {
              is NotificationsClicked -> navigateToNotifications()
              is PrivacyClicked -> startActivity(
                Intent(
                  Intent.ACTION_VIEW,
                  Uri.parse("https://www.freeprivacypolicy.com/privacy/view/40f932df1dfc9af1b04df367aa6f14f0")
                )
              )

              is LogoutClicked -> dialogData = logoutDialogData
              is SupportClicked -> view?.findNavController()?.navigate(R.id.action_settingsFragment_to_supporterFragment)
              else -> viewModel.onAction(it)
            }
          }

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
              title = { Text(data.title) },
            )
          }

          if(state.passwordDialogVisible) {
            var password by remember { mutableStateOf("") }

            AlertDialog(
              onDismissRequest = { viewModel.onAction(DeleteAccountCanceled) },
              confirmButton = {
                TextButton(onClick = {
                  if(password.isNotBlank()) {
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
                    visualTransformation = if(!passwordVisibility) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = true,
                    trailingIcon = {
                      IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                        Icon(
                          imageVector  = if (passwordVisibility) Filled.Visibility else Filled.VisibilityOff,
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

          if(state.modalVisible) {
            Dialog(
              onDismissRequest = { },
              DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(100.dp)
                  .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(8.dp)
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
      }
    }
  }

  private fun navigateToNotifications() {
    val intent = Intent()
    intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"

    //for Android 5-7
    intent.putExtra("app_package", activity?.packageName)
    intent.putExtra("app_uid", activity?.applicationInfo?.uid)

    // for Android O
    intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)

    startActivity(intent)
  }

  private fun doLogout() {
    context?.let { FirebaseAnalytics.getInstance(it).logEvent("logout_clicked", null) }
    FirebaseCrashlytics.getInstance().sendUnsentReports()
    userSessionRepository.logOut()
    (activity as? MainActivity)?.showLogin()
  }
}

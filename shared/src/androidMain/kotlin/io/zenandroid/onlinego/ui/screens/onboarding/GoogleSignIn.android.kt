package io.zenandroid.onlinego.ui.screens.onboarding

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

private const val SERVER_CLIENT_ID =
  "870935345166-6j2s6i9adl64ms3ta4k9n4flkqjhs229.apps.googleusercontent.com"

@Composable
internal actual fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): () -> Unit {
  val activity = LocalActivity.current
  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode != Activity.RESULT_OK) {
      onResult(GoogleSignInResult.Cancelled)
      return@rememberLauncherForActivityResult
    }
    try {
      val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        .getResult(ApiException::class.java)
      onResult(GoogleSignInResult.Success(account?.serverAuthCode))
    } catch (e: ApiException) {
      onResult(GoogleSignInResult.Failure(e.statusCode, e))
    }
  }
  return {
    activity?.let {
      val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestServerAuthCode(SERVER_CLIENT_ID)
        .requestScopes(Scope(Scopes.OPEN_ID), Scope(Scopes.EMAIL), Scope(Scopes.PROFILE))
        .build()
      launcher.launch(GoogleSignIn.getClient(it, options).signInIntent)
    }
  }
}

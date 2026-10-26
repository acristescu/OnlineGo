package io.zenandroid.onlinego.ui.screens.onboarding

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger

@Composable
internal actual fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): () -> Unit = {
  Logger.w("Google sign-in is not available on iOS yet", tag = "GoogleSignIn")
  onResult(GoogleSignInResult.Cancelled)
}

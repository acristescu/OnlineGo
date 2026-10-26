package io.zenandroid.onlinego.ui.screens.onboarding

import androidx.compose.runtime.Composable

internal sealed interface GoogleSignInResult {
  data class Success(val serverAuthCode: String?) : GoogleSignInResult
  data class Failure(val statusCode: Int, val cause: Throwable) : GoogleSignInResult
  data object Cancelled : GoogleSignInResult
}

@Composable
internal expect fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): () -> Unit

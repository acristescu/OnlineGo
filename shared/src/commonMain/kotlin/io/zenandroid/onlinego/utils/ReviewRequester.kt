package io.zenandroid.onlinego.utils

import androidx.compose.runtime.staticCompositionLocalOf

fun interface ReviewRequester {
  suspend fun requestReview()
}

val LocalReviewRequester = staticCompositionLocalOf { ReviewRequester {} }

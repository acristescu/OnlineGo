package io.zenandroid.onlinego.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.repositories.ReviewPromptRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages the review prompt flow following Google's guidelines:
 * - Uses Google Play In-App Review API when available
 * - Falls back to Play Store redirect if in-app review fails
 * - Tracks analytics events for the review flow
 */
class ReviewPromptManager(
  private val context: Context,
  private val reviewPromptRepository: ReviewPromptRepository,
  private val applicationScope: CoroutineScope
) {

  private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
  private val analytics = FirebaseAnalytics.getInstance(context)

  /**
   * Attempts to show the review prompt following Google's best practices.
   * This should be called when the user wins a game and meets the criteria.
   */
  suspend fun requestReview(activity: Activity) {
    try {
      // Check if we should prompt for review
      if (!reviewPromptRepository.shouldPromptForReview()) {
        FirebaseCrashlytics.getInstance().log("Review prompt conditions not met")
        return
      }

      // Record that we're showing the prompt
      reviewPromptRepository.recordReviewPromptShown()

      // Try to use Google Play In-App Review API first
      val reviewInfo = requestReviewInfo()
      if (reviewInfo != null) {
        launchReviewFlow(activity, reviewInfo)
      } else {
        // Fallback to Play Store redirect
        launchPlayStoreReview(activity)
      }

    } catch (e: Exception) {
      FirebaseCrashlytics.getInstance().recordException(e)
      // Fallback to Play Store redirect on any error
      launchPlayStoreReview(activity)
    }
  }

  /**
   * Requests review info from Google Play In-App Review API
   */
  private suspend fun requestReviewInfo(): ReviewInfo? =
    suspendCancellableCoroutine { continuation ->
      reviewManager.requestReviewFlow().addOnCompleteListener { request ->
        if (request.isSuccessful) {
          continuation.resume(request.result, null)
        } else {
          FirebaseCrashlytics.getInstance()
            .log("In-app review request failed: ${request.exception?.message}")
          continuation.resume(null)
        }
      }

      continuation.invokeOnCancellation {
        // Handle cancellation if needed
      }
    }

  /**
   * Launches the Google Play In-App Review flow
   */
  private fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo) {
    reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener { launch ->
      if (launch.isSuccessful) {
        // User completed the review flow
        applicationScope.launch(Dispatchers.IO) {
          reviewPromptRepository.recordReviewPromptRated()
        }
        analytics.logEvent("review_prompt_completed_in_app", null)
        FirebaseCrashlytics.getInstance().log("In-app review completed successfully")
      } else {
        // User dismissed or there was an error
        applicationScope.launch(Dispatchers.IO) {
          reviewPromptRepository.recordReviewPromptDismissed()
        }
        analytics.logEvent("review_prompt_dismissed_in_app", null)
        FirebaseCrashlytics.getInstance()
          .log("In-app review dismissed or failed: ${launch.exception?.message}")
      }
    }
  }

  /**
   * Fallback to Play Store review page
   */
  private fun launchPlayStoreReview(activity: Activity) {
    try {
      val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
        setPackage("com.android.vending") // Try to open in Play Store app
      }

      if (intent.resolveActivity(activity.packageManager) != null) {
        activity.startActivity(intent)
        analytics.logEvent("review_prompt_redirected_to_play_store", null)
        FirebaseCrashlytics.getInstance().log("Redirected to Play Store for review")
      } else {
        // Fallback to browser
        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
          data = Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
        }
        activity.startActivity(browserIntent)
        analytics.logEvent("review_prompt_redirected_to_browser", null)
        FirebaseCrashlytics.getInstance().log("Redirected to browser for review")
      }

      // Record as dismissed since we can't track completion from Play Store
      applicationScope.launch(Dispatchers.IO) {
        reviewPromptRepository.recordReviewPromptDismissed()
      }

    } catch (e: Exception) {
      FirebaseCrashlytics.getInstance().recordException(e)
      analytics.logEvent("review_prompt_fallback_failed", null)
    }
  }

  /**
   * Manually mark that the user has rated the app (useful if they rate externally)
   */
  suspend fun markAppAsRated() {
    reviewPromptRepository.recordReviewPromptRated()
    analytics.logEvent("app_rated_externally", null)
  }

  /**
   * Reset the review prompt state (useful for testing)
   */
  suspend fun resetReviewState() {
    reviewPromptRepository.resetReviewPromptState()
    analytics.logEvent("review_state_reset", null)
  }

  /**
   * Force show review prompt (useful for testing)
   */
  suspend fun forceShowReviewPrompt(activity: Activity) {
    try {
      reviewPromptRepository.recordReviewPromptShown()
      val reviewInfo = requestReviewInfo()
      if (reviewInfo != null) {
        launchReviewFlow(activity, reviewInfo)
      } else {
        launchPlayStoreReview(activity)
      }
    } catch (e: Exception) {
      FirebaseCrashlytics.getInstance().recordException(e)
      launchPlayStoreReview(activity)
    }
  }
}

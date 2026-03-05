package io.zenandroid.onlinego.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.data.repositories.ReviewPromptRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

  private val reviewManager = ReviewManagerFactory.create(context)
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

      // Use the ktx suspend extension which properly handles the Task
      val reviewInfo = reviewManager.requestReview()
      reviewManager.launchReview(activity, reviewInfo)

      // Note: Google's API does not tell us whether the user actually reviewed.
      // The flow always "completes successfully" regardless of user action.
      // We record it as shown; we cannot know if they rated.
      analytics.logEvent("review_prompt_completed_in_app", null)
      FirebaseCrashlytics.getInstance().log("In-app review flow completed")

    } catch (e: Exception) {
      FirebaseCrashlytics.getInstance().recordException(e)
      FirebaseCrashlytics.getInstance()
        .log("In-app review failed: ${e.message}, falling back to Play Store")
      // Fallback to Play Store redirect on any error
      launchPlayStoreReview(activity)
    }
  }

  /**
   * Fallback to Play Store review page
   */
  private fun launchPlayStoreReview(activity: Activity) {
    try {
      val intent = Intent(Intent.ACTION_VIEW).apply {
        data = "https://play.google.com/store/apps/details?id=${activity.packageName}".toUri()
        setPackage("com.android.vending") // Try to open in Play Store app
      }

      if (intent.resolveActivity(activity.packageManager) != null) {
        activity.startActivity(intent)
        analytics.logEvent("review_prompt_redirected_to_play_store", null)
        FirebaseCrashlytics.getInstance().log("Redirected to Play Store for review")
      } else {
        // Fallback to browser
        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
          data = "https://play.google.com/store/apps/details?id=${activity.packageName}".toUri()
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
   * Force show review prompt (useful for testing).
   * Returns a diagnostic result that can be displayed in the UI.
   */
  suspend fun forceShowReviewPrompt(activity: Activity): ReviewDiagnosticResult {
    val steps = mutableListOf<String>()
    return try {
      steps.add("Starting forced review flow...")

      steps.add("Requesting ReviewInfo from Google Play...")
      val reviewInfo = reviewManager.requestReview()
      steps.add("✅ ReviewInfo obtained successfully: $reviewInfo")

      steps.add("Launching review flow...")
      reviewManager.launchReview(activity, reviewInfo)
      steps.add("✅ Review flow completed (note: Google does not reveal if user actually reviewed)")

      reviewPromptRepository.recordReviewPromptShown()
      steps.add("✅ Recorded prompt shown")

      ReviewDiagnosticResult(
        success = true,
        steps = steps,
        error = null
      )
    } catch (e: Exception) {
      steps.add("❌ Error: ${e.javaClass.simpleName}: ${e.message}")
      steps.add("Stack trace: ${e.stackTraceToString().take(500)}")
      FirebaseCrashlytics.getInstance().recordException(e)

      ReviewDiagnosticResult(
        success = false,
        steps = steps,
        error = e
      )
    }
  }
}

data class ReviewDiagnosticResult(
  val success: Boolean,
  val steps: List<String>,
  val error: Exception?
) {
  fun toDisplayString(): String = buildString {
    appendLine(if (success) "=== REVIEW FLOW SUCCEEDED ===" else "=== REVIEW FLOW FAILED ===")
    appendLine()
    steps.forEachIndexed { index, step ->
      appendLine("${index + 1}. $step")
    }
    if (error != null) {
      appendLine()
      appendLine("Error type: ${error.javaClass.name}")
      appendLine("Error message: ${error.message}")
    }
  }
}

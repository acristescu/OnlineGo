package io.zenandroid.onlinego.utils

import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.repositories.ReviewPromptRepository
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene

class IosReviewPromptManager(
  private val reviewPromptRepository: ReviewPromptRepository,
  private val analytics: Analytics,
) {

  suspend fun requestReview(host: UIViewController) {
    if (!reviewPromptRepository.shouldPromptForReview()) {
      Logger.i("Review prompt conditions not met", tag = "IosReviewPromptManager")
      return
    }
    val scene = host.windowScene() ?: run {
      Logger.i("No window scene for review prompt", tag = "IosReviewPromptManager")
      return
    }
    reviewPromptRepository.recordReviewPromptShown()
    SKStoreReviewController.requestReviewInScene(scene)
    analytics.logEvent("review_prompt_completed_in_app")
  }

  suspend fun forceShowReviewPrompt(host: UIViewController): ReviewDiagnosticResult {
    val steps = mutableListOf("Starting forced review flow...")
    val scene = host.windowScene()
      ?: return ReviewDiagnosticResult(false, steps + "❌ No UIWindowScene for the app window", null)
    steps.add("✅ UIWindowScene found")

    SKStoreReviewController.requestReviewInScene(scene)
    steps.add("✅ SKStoreReviewController asked to show the prompt (iOS decides whether it appears; it never shows in TestFlight)")

    reviewPromptRepository.recordReviewPromptShown()
    steps.add("✅ Recorded prompt shown")

    return ReviewDiagnosticResult(true, steps, null)
  }

  private fun UIViewController.windowScene(): UIWindowScene? = view.window?.windowScene
}

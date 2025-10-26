package io.zenandroid.onlinego.data.repositories

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

val Context.reviewPromptDataStore by preferencesDataStore(
  name = "review_prompt"
)

class ReviewPromptRepository(
  private val context: Context,
  private val applicationScope: CoroutineScope,
  private val userSessionRepository: UserSessionRepository,
) {

  private val dataStore = context.reviewPromptDataStore
  private val analytics = FirebaseAnalytics.getInstance(context)

  companion object {
    private val APP_FIRST_LAUNCH_TIME = longPreferencesKey("app_first_launch_time")
    private val LAST_REVIEW_PROMPT_TIME = longPreferencesKey("last_review_prompt_time")
    private val REVIEW_PROMPT_DISMISSED = booleanPreferencesKey("review_prompt_dismissed")
    private val REVIEW_PROMPT_RATED = booleanPreferencesKey("review_prompt_rated")

    // Configuration constants
    private const val DAYS_BEFORE_FIRST_PROMPT = 1
    private const val COOLDOWN_DAYS = 1
  }

  init {
    // Initialize first launch time if not set
    applicationScope.launch(Dispatchers.IO) {
      val prefs = dataStore.data.first()
      if (prefs[APP_FIRST_LAUNCH_TIME] == null) {
        dataStore.edit { preferences ->
          preferences[APP_FIRST_LAUNCH_TIME] = System.currentTimeMillis()
        }
      }
    }
  }

  /**
   * Checks if the user should be prompted for a review based on:
   * 1. App has been used for the configured number of days
   * 2. Not in cooldown period (2 weeks after dismissal)
   * 3. User hasn't already rated the app
   */
  suspend fun shouldPromptForReview(): Boolean {
    if (userSessionRepository.userIdObservable.blockingFirst() != 89194L) {
      return false
    }
    val prefs = dataStore.data.first()

    val firstLaunchTime = prefs[APP_FIRST_LAUNCH_TIME] ?: return false
    val lastPromptTime = prefs[LAST_REVIEW_PROMPT_TIME]
    val isDismissed = prefs[REVIEW_PROMPT_DISMISSED] ?: false
    val isRated = prefs[REVIEW_PROMPT_RATED] ?: false

    // Don't prompt if user already rated
    if (isRated) {
      return false
    }

    val currentTime = System.currentTimeMillis()
    val daysSinceFirstLaunch = TimeUnit.MILLISECONDS.toDays(currentTime - firstLaunchTime)

    // Check if enough days have passed since first launch
    if (daysSinceFirstLaunch < DAYS_BEFORE_FIRST_PROMPT) {
      return false
    }

    // If user dismissed, check cooldown period
    if (isDismissed && lastPromptTime != null) {
      val daysSinceLastPrompt = TimeUnit.MILLISECONDS.toDays(currentTime - lastPromptTime)
      if (daysSinceLastPrompt < COOLDOWN_DAYS) {
        return false
      }
    }

    return true
  }

  /**
   * Records that a review prompt was shown
   */
  suspend fun recordReviewPromptShown() {
    dataStore.edit { preferences ->
      preferences[LAST_REVIEW_PROMPT_TIME] = System.currentTimeMillis()
    }

    // Analytics event
    analytics.logEvent("review_prompt_shown", null)
    FirebaseCrashlytics.getInstance().log("Review prompt shown to user")
  }

  /**
   * Records that the user dismissed the review prompt
   */
  suspend fun recordReviewPromptDismissed() {
    dataStore.edit { preferences ->
      preferences[REVIEW_PROMPT_DISMISSED] = true
    }

    // Analytics event
    analytics.logEvent("review_prompt_dismissed", null)
    FirebaseCrashlytics.getInstance().log("Review prompt dismissed by user")
  }

  /**
   * Records that the user rated the app (either through in-app review or external)
   */
  suspend fun recordReviewPromptRated() {
    dataStore.edit { preferences ->
      preferences[REVIEW_PROMPT_RATED] = true
    }

    // Analytics event
    analytics.logEvent("review_prompt_rated", null)
    FirebaseCrashlytics.getInstance().log("User rated the app")
  }

  /**
   * Resets the review prompt state (useful for testing or if user wants to be prompted again)
   */
  suspend fun resetReviewPromptState() {
    dataStore.edit { preferences ->
      preferences.remove(REVIEW_PROMPT_DISMISSED)
      preferences.remove(REVIEW_PROMPT_RATED)
      preferences.remove(LAST_REVIEW_PROMPT_TIME)
    }

    // Analytics event
    analytics.logEvent("review_prompt_reset", null)
    FirebaseCrashlytics.getInstance().log("Review prompt state reset")
  }

  /**
   * Gets the current review prompt state for debugging
   */
  suspend fun getReviewPromptState(): ReviewPromptState {
    val prefs = dataStore.data.first()
    return ReviewPromptState(
      firstLaunchTime = prefs[APP_FIRST_LAUNCH_TIME],
      lastPromptTime = prefs[LAST_REVIEW_PROMPT_TIME],
      isDismissed = prefs[REVIEW_PROMPT_DISMISSED] ?: false,
      isRated = prefs[REVIEW_PROMPT_RATED] ?: false
    )
  }
}

data class ReviewPromptState(
  val firstLaunchTime: Long?,
  val lastPromptTime: Long?,
  val isDismissed: Boolean,
  val isRated: Boolean
)

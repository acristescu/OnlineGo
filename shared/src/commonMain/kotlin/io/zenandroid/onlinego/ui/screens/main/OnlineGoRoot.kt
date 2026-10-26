package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zenandroid.onlinego.data.billing.SupporterProduct
import io.zenandroid.onlinego.data.billing.SupporterPurchase
import io.zenandroid.onlinego.data.model.AppTheme
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.ui.theme.LocalPreloadedImages
import io.zenandroid.onlinego.ui.theme.LocalThemeSettings
import io.zenandroid.onlinego.ui.theme.PreloadedImages
import io.zenandroid.onlinego.ui.theme.ThemeSettings
import io.zenandroid.onlinego.ui.theme.loadPreloadedImages
import io.zenandroid.onlinego.utils.LocalReviewRequester
import io.zenandroid.onlinego.utils.ReviewDiagnosticResult
import io.zenandroid.onlinego.utils.ReviewRequester
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnlineGoRoot(
  versionCode: Int,
  reviewRequester: ReviewRequester,
  onForceReviewPrompt: suspend () -> ReviewDiagnosticResult,
  onTestNotification: suspend (Game) -> Unit,
  onLaunchBillingFlow: suspend (SupporterProduct, SupporterPurchase?) -> Unit,
  onFinish: () -> Unit,
  viewModel: AppViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LifecycleResumeEffect(viewModel) {
    viewModel.onResume()
    onPauseOrDispose { viewModel.onPause() }
  }

  val systemDark = isSystemInDarkTheme()
  val themeSettings = ThemeSettings(
    isDarkTheme = when (state.appTheme) {
      AppTheme.LIGHT -> false
      AppTheme.DARK -> true
      AppTheme.SYSTEM_DEFAULT, null -> systemDark
    },
    boardTheme = state.boardTheme ?: BoardTheme.WOOD,
    dynamicColors = true,
    showCoordinates = state.showCoordinates,
  )

  val density = LocalDensity.current
  val preloadedImages by produceState(
    PreloadedImages(null, null, null),
    themeSettings.boardTheme,
    themeSettings.isDarkTheme,
  ) {
    value = loadPreloadedImages(themeSettings.boardTheme, themeSettings.isDarkTheme, density)
  }

  SystemBarsAppearance(themeSettings.isDarkTheme)

  val hasCompletedOnboarding = state.hasCompletedOnboarding ?: return
  CompositionLocalProvider(
    LocalThemeSettings provides themeSettings,
    LocalPreloadedImages provides preloadedImages,
    LocalReviewRequester provides reviewRequester,
  ) {
    OnlineGoApp(
      darkTheme = themeSettings.isDarkTheme,
      isLoggedIn = state.isLoggedIn == true,
      hasCompletedOnboarding = hasCompletedOnboarding,
      versionCode = versionCode,
      onForceReviewPrompt = onForceReviewPrompt,
      onTestNotification = onTestNotification,
      onLaunchBillingFlow = onLaunchBillingFlow,
      onFinish = onFinish,
    )
  }
}

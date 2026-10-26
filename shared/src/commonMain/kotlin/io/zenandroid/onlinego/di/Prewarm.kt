package io.zenandroid.onlinego.di

import co.touchlab.kermit.Logger
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.data.ogs.OGSWebSocketService
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.ui.screens.mygames.composables.HEADER_AVATAR_SIZE
import io.zenandroid.onlinego.ui.screens.mygames.composables.HEADER_AVATAR_URL_WIDTH
import io.zenandroid.onlinego.utils.processGravatarURL
import org.koin.mp.KoinPlatform.getKoin

/**
 * Resolves the slow singletons and warms the caches the first screen reads, so the app does not
 * pay for them on its first frame. Call once Koin is started, off the main thread.
 */
fun prewarmApp(context: PlatformContext, density: Float) {
  Logger.d(tag = "AppInit") { "Pre-warming network stack" }
  try {
    getKoin().get<OGSRestService>()
    getKoin().get<OGSWebSocketService>()
    preloadOwnAvatar(context, density)
    Logger.d(tag = "AppInit") { "Network stack pre-warmed" }
  } catch (e: Exception) {
    Logger.e("Error pre-warming Koin dependencies", e, "AppInit")
  }
  RulesManager.coordinateToCell("A1")
  Logger.i("Done pre-warming network stack", tag = "AppInit")
}

/**
 * Warms the image caches so the home screen header renders the avatar on its first frame
 * rather than showing the placeholder and swapping. Mirrors the two sizes
 * [HomeScreenHeader][io.zenandroid.onlinego.ui.screens.mygames.composables.HomeScreenHeader]
 * uses, so the resulting memory cache entry is the one it will look up.
 */
private fun preloadOwnAvatar(context: PlatformContext, density: Float) {
  val icon = getKoin().get<UserSessionRepository>().uiConfig?.user?.icon ?: return
  val urlWidth = (HEADER_AVATAR_URL_WIDTH.value * density).toInt()
  val displaySize = (HEADER_AVATAR_SIZE.value * density).toInt()
  SingletonImageLoader.get(context).enqueue(
    ImageRequest.Builder(context)
      .data(processGravatarURL(icon, urlWidth))
      .size(displaySize)
      .build()
  )
}

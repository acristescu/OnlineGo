package io.zenandroid.onlinego.ui.composables

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.NetworkFetcher
import kotlin.test.Test
import kotlin.test.assertTrue

class ImageLoaderTest {

  @Test
  fun theImageLoaderCanFetchFromTheNetwork() {
    val factories = SingletonImageLoader.get(PlatformContext.INSTANCE).components.fetcherFactories
    assertTrue(factories.any { (factory, _) -> factory is NetworkFetcher.Factory })
  }
}

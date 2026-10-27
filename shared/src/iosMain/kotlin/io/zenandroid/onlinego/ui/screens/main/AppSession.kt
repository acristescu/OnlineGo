package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

internal class AppSession {
  private val generation = MutableStateFlow<Generation?>(Generation())

  @Composable
  fun Content(content: @Composable () -> Unit) {
    val current = generation.collectAsState().value ?: return
    DisposableEffect(current) {
      onDispose { current.disposed.complete(Unit) }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides current, content = content)
  }

  suspend fun restart(reset: suspend () -> Unit) {
    val old = generation.value ?: return
    generation.value = null
    old.disposed.await()
    old.viewModelStore.clear()
    reset()
    generation.value = Generation()
  }

  private class Generation : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    val disposed = CompletableDeferred<Unit>()
  }
}

internal val appSession = AppSession()

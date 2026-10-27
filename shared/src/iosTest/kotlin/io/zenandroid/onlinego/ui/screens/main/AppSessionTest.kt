package io.zenandroid.onlinego.ui.screens.main

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

@OptIn(ExperimentalTestApi::class)
class AppSessionTest {

  private class Probe(val events: MutableList<String>) : ViewModel() {
    override fun onCleared() {
      events += "cleared"
    }
  }

  @Test
  fun restartDisposesTheContentAndClearsItsViewModelsBeforeResetting() = runComposeUiTest {
    val session = AppSession()
    val events = mutableListOf<String>()
    val probes = mutableListOf<Probe>()
    setContent {
      session.Content {
        val probe = viewModel { Probe(events) }
        DisposableEffect(probe) {
          probes += probe
          events += "composed"
          onDispose { events += "disposed" }
        }
      }
    }
    waitForIdle()

    CoroutineScope(Dispatchers.Unconfined).launch { session.restart { events += "reset" } }
    waitUntil { probes.size == 2 }

    assertEquals(listOf("composed", "disposed", "cleared", "reset", "composed"), events)
    assertNotSame(probes[0], probes[1])
  }
}

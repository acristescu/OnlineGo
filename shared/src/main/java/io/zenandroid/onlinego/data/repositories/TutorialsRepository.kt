package io.zenandroid.onlinego.data.repositories

import android.content.Context
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.local.TutorialGroup
import io.zenandroid.onlinego.utils.appJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream

class TutorialsRepository(
  private val appCoroutineScope: CoroutineScope,
  private val settingsRepository: SettingsRepository,
  private val context: Context,
) : SocketConnectedRepository {

  private lateinit var hardcodedTutorialsData: List<TutorialGroup>

  /** Tutorial.name doubles as a stable id here - see the KDoc on Tutorial. */
  private val _completedTutorialsNames = MutableStateFlow<Set<String>>(emptySet())
  val completedTutorialsNames: StateFlow<Set<String>> = _completedTutorialsNames.asStateFlow()

  init {
    appCoroutineScope.launch(Dispatchers.IO) {
      settingsRepository.completedTutorialsFlow.collect {
        _completedTutorialsNames.value = it
      }
      if (!this@TutorialsRepository::hardcodedTutorialsData.isInitialized) {
        hardcodedTutorialsData = readJSONFromResources()
      }
    }
  }

  suspend fun loadTutorial(tutorialName: String): Tutorial? {
    if (!this::hardcodedTutorialsData.isInitialized) {
      hardcodedTutorialsData = readJSONFromResources()
    }
    hardcodedTutorialsData.forEach { group ->
      group.tutorials.find {
        it.name == tutorialName
      }?.let {
        return it
      }
    }
    return null
  }

  suspend fun getTutorialGroups(): List<TutorialGroup> {
    if (!this::hardcodedTutorialsData.isInitialized) {
      hardcodedTutorialsData = readJSONFromResources()
    }
    return hardcodedTutorialsData
  }

  @OptIn(ExperimentalSerializationApi::class)
  private suspend fun readJSONFromResources(): List<TutorialGroup> =
    context.assets.open("tutorials.json").use {
      appJson.decodeFromStream<List<TutorialGroup>>(it)
    }

  fun markTutorialCompleted(tutorial: Tutorial) {
    appCoroutineScope.launch(Dispatchers.IO) {
      val current = _completedTutorialsNames.value
      if (!current.contains(tutorial.name)) {
        settingsRepository.setCompletedTutorials(current + tutorial.name)
      }
    }
  }

  override fun onSocketConnected() {
  }

  override fun onSocketDisconnected() {
  }
}

package io.zenandroid.onlinego.data.repositories

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.data.model.local.Tutorial
import io.zenandroid.onlinego.data.model.local.TutorialGroup
import io.zenandroid.onlinego.data.model.local.TutorialStep
import io.zenandroid.onlinego.data.model.local.TutorialStep.GameExample
import io.zenandroid.onlinego.data.model.local.TutorialStep.Interactive
import io.zenandroid.onlinego.data.model.local.TutorialStep.Lesson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.buffer
import okio.source

class TutorialsRepository(
  private val appCoroutineScope: CoroutineScope,
  private val settingsRepository: SettingsRepository
) : SocketConnectedRepository {

  private val moshiAdapter by lazy {
    Moshi.Builder()
      .add(
        PolymorphicJsonAdapterFactory.of(TutorialStep::class.java, "type")
          .withSubtype(Interactive::class.java, "Interactive")
          .withSubtype(Lesson::class.java, "Lesson")
          .withSubtype(GameExample::class.java, "Game")
      )
      .addLast(KotlinJsonAdapterFactory())
      .build()
      .adapter<List<TutorialGroup>>(
        Types.newParameterizedType(
          List::class.java,
          TutorialGroup::class.java
        )
      )
  }

  private lateinit var hardcodedTutorialsData: List<TutorialGroup>
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

  private suspend fun readJSONFromResources(): List<TutorialGroup> {
    OnlineGoApplication.instance.assets.open("tutorials.json").source().buffer().use {
      return moshiAdapter.fromJson(it)!!
    }
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
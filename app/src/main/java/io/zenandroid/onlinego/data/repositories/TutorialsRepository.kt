package io.zenandroid.onlinego.data.repositories

import android.preference.PreferenceManager
import androidx.core.content.edit
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.buffer
import okio.source

private const val COMPLETED_TUTORIALS_KEY = "COMPLETED_TUTORIALS_KEY"

class TutorialsRepository(
  private val appCoroutineScope: CoroutineScope
) : SocketConnectedRepository {

  private val moshiAdapter = Moshi.Builder()
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

  private val prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance)

  private lateinit var hardcodedTutorialsData: List<TutorialGroup>
  private val _completedTutorialsNames = MutableStateFlow<Set<String>>(emptySet())
  val completedTutorialsNames: StateFlow<Set<String>> = _completedTutorialsNames.asStateFlow()

  init {
    appCoroutineScope.launch(Dispatchers.IO) {
      val completed = prefs.getStringSet(COMPLETED_TUTORIALS_KEY, emptySet())!!
      _completedTutorialsNames.value = completed
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
    _completedTutorialsNames.update {
      if (it.contains(tutorial.name)) {
        return@update it
      }
      prefs.edit {
        putStringSet(COMPLETED_TUTORIALS_KEY, it + tutorial.name)
      }
      it + tutorial.name
    }
  }

  override fun onSocketConnected() {
  }

  override fun onSocketDisconnected() {
  }
}
package io.zenandroid.onlinego.data.repositories

import android.content.Context
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
    var context = OnlineGoApplication.instance;
    var rawList = context.assets.open("tutorials.json").source().buffer().use {
      moshiAdapter.fromJson(it)!!
    }

    return rawList.map { group ->
      group.copy(
        name = context.getLocalizedString(group.name),
        tutorials = group.tutorials.map { tutorial ->
          tutorial.copy(
            name = context.getLocalizedString(tutorial.name),
            steps = tutorial.steps.map { step ->
              translateStep(context, step)
            }
          )
        }
      )
    }
  }

  private fun translateStep(context: Context, step: TutorialStep) : TutorialStep {
    return when (step) {
      is TutorialStep.Lesson -> step.copy(
        name = context.getLocalizedString(step.name),
        pages = step.pages.map { page ->
          page.copy(
            text = context.getLocalizedString(page.text)
          )
        }
      )
      is TutorialStep.Interactive -> step.copy(
        name = context.getLocalizedString(step.name),
        text = context.getLocalizedString(step.text),
        branches = step.branches.map { branch ->
          translateNode(context, branch)
        }
      )
      is TutorialStep.GameExample -> step.copy(
        name = context.getLocalizedString(step.name),
        text = context.getLocalizedString(step.text)
      )
    }
  }

  private fun translateNode(context: Context, node: io.zenandroid.onlinego.data.model.local.Node) : io.zenandroid.onlinego.data.model.local.Node {
    return node.copy(
      move = node.move,
      reply = node.reply,
      message = node.message?.let { context.getLocalizedString(it) },
      success = node.success,
      failed = node.failed,
      branches = node.branches?.map { branch ->
        translateNode(context, branch)
      }
    )
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

fun Context.getLocalizedString(resourceName: String): String {
  val resId = resources.getIdentifier(resourceName, "string", packageName)
  return if (resId != 0) getString(resId) else resourceName
}

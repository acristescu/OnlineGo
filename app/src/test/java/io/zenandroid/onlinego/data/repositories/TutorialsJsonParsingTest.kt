package io.zenandroid.onlinego.data.repositories

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.data.model.local.Node
import io.zenandroid.onlinego.data.model.local.TutorialGroup
import io.zenandroid.onlinego.data.model.local.TutorialStep
import okio.buffer
import okio.source
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * tutorials.json's translatable fields (group/tutorial name, step text, page text, node message)
 * hold string resource *names*, resolved at render time (see resolveTutorialText). This confirms
 * every one of them has a matching entry in strings.xml, since a typo here would otherwise only
 * ever surface as a silently-wrong raw key shown on a real device.
 */
class TutorialsJsonParsingTest {

  private val moshiAdapter = Moshi.Builder()
    .add(
      PolymorphicJsonAdapterFactory.of(TutorialStep::class.java, "type")
        .withSubtype(TutorialStep.Interactive::class.java, "Interactive")
        .withSubtype(TutorialStep.Lesson::class.java, "Lesson")
        .withSubtype(TutorialStep.GameExample::class.java, "Game")
    )
    .addLast(KotlinJsonAdapterFactory())
    .build()
    .adapter<List<TutorialGroup>>(
      Types.newParameterizedType(List::class.java, TutorialGroup::class.java)
    )

  private val declaredStringResourceNames: Set<String> by lazy {
    Regex("""<string\s+name="([^"]+)"""")
      .findAll(File("src/main/res/values/strings.xml").readText())
      .map { it.groupValues[1] }
      .toSet()
  }

  private fun assertResolvable(resourceName: String) {
    assertTrue(
      "Missing string resource for tutorial content: \"$resourceName\"",
      resourceName in declaredStringResourceNames
    )
  }

  private fun assertNode(node: Node) {
    node.message?.let { assertResolvable(it) }
    node.branches?.forEach { assertNode(it) }
  }

  @Test
  fun `every translatable tutorials json field resolves to a real string resource`() {
    val groups = File("src/main/assets/tutorials.json").source().buffer().use {
      moshiAdapter.fromJson(it)!!
    }

    assertTrue("Expected at least one tutorial group", groups.isNotEmpty())

    groups.forEach { group ->
      assertResolvable(group.name)
      group.tutorials.forEach { tutorial ->
        assertResolvable(tutorial.name)
        tutorial.steps.forEach { step ->
          when (step) {
            is TutorialStep.Interactive -> {
              assertResolvable(step.text)
              step.branches.forEach { assertNode(it) }
            }

            is TutorialStep.Lesson -> step.pages.forEach { assertResolvable(it.text) }
            is TutorialStep.GameExample -> assertResolvable(step.text)
          }
        }
      }
    }
  }
}

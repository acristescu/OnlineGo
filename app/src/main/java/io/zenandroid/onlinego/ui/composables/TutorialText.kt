package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import io.zenandroid.onlinego.resources.Res
import io.zenandroid.onlinego.resources.allStringResources
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves a bundled tutorial content string - which is actually a string resource *name* (e.g.
 * "tutorial_basics_title"), not literal text - to its localized value. Re-resolves on every
 * recomposition, so this survives an in-app language change with no caching needed. Falls back to
 * the raw name unchanged if there's no matching resource.
 */
@Composable
fun resolveTutorialText(resourceName: String): String =
  Res.allStringResources[resourceName]?.let { stringResource(it) } ?: resourceName

@Composable
fun resolveTutorialTextOrNull(resourceName: String?): String? =
  resourceName?.let { resolveTutorialText(it) }

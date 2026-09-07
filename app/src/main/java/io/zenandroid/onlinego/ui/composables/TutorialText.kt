package io.zenandroid.onlinego.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource

/**
 * Resolves a bundled tutorial content string - which is actually a string resource *name* (e.g.
 * "tutorial_basics_title"), not literal text - to its localized value. Re-resolves on every
 * recomposition, so this survives an in-app language change with no caching needed. Falls back to
 * the raw name unchanged if there's no matching resource.
 */
@Composable
fun resolveTutorialText(resourceName: String): String {
  val context = LocalContext.current
  val resources = LocalResources.current
  val resId = remember(resourceName) {
    resources.getIdentifier(resourceName, "string", context.packageName)
  }
  return if (resId != 0) stringResource(resId) else resourceName
}

@Composable
fun resolveTutorialTextOrNull(resourceName: String?): String? =
  resourceName?.let { resolveTutorialText(it) }

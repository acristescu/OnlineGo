package io.zenandroid.onlinego.ui.composables

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/**
 * A user facing text that has not been resolved yet. It allows view models to describe what should
 * be shown without holding on to a Context, while still supporting format arguments.
 *
 * Use [textResource] to build one with arguments and [resolve] to turn it into a String from a
 * composable.
 */
@Immutable
data class TextResource(
  @StringRes val resId: Int,
  val args: List<Any> = emptyList(),
)

fun textResource(@StringRes resId: Int, vararg args: Any) = TextResource(resId, args.toList())

@Composable
fun TextResource.resolve(): String =
  if (args.isEmpty()) stringResource(resId) else stringResource(resId, *args.toTypedArray())

@Composable
fun TextResource?.resolveOrNull(): String? = this?.resolve()

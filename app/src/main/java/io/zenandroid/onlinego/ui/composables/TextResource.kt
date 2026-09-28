package io.zenandroid.onlinego.ui.composables

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalResources
import io.zenandroid.onlinego.utils.recordException

/**
 * A user facing text that has not been resolved yet. It allows view models to describe what should
 * be shown without holding on to a Context, while still supporting format arguments.
 *
 * Use [textResource] to build one with arguments and [resolve] to turn it into a String from a
 * composable.
 *
 * [resId] is only valid for the build that produced it, so a TextResource must never be persisted
 * or sent off-device.
 */
@Immutable
data class TextResource(
  @StringRes val resId: Int,
  val args: List<Any> = emptyList(),
)

fun textResource(@StringRes resId: Int, vararg args: Any) = TextResource(resId, args.toList())

@Composable
fun TextResource.resolve(): String {
  val resources = LocalResources.current
  return try {
    if (args.isEmpty()) resources.getString(resId)
    else resources.getString(resId, *args.toTypedArray())
  } catch (e: Resources.NotFoundException) {
    recordException(e)
    ""
  }
}

@Composable
fun TextResource?.resolveOrNull(): String? = this?.resolve()

/**
 * User facing text that is either one of our own string resources or a literal we were handed at
 * runtime and cannot translate - typically an error message from the server.
 */
@Immutable
sealed interface UiText {
  data class Literal(val text: String) : UiText
  data class FromResource(val resource: TextResource) : UiText
}

fun uiText(@StringRes resId: Int, vararg args: Any): UiText =
  UiText.FromResource(TextResource(resId, args.toList()))

fun literalText(text: String): UiText = UiText.Literal(text)

@Composable
fun UiText.resolve(): String = when (this) {
  is UiText.Literal -> text
  is UiText.FromResource -> resource.resolve()
}

package io.zenandroid.onlinego.ui.composables

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect as ViewRect
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Card
import androidx.compose.material.ExposedDropdownMenuBoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Adapted from androidx.compose.material3.ExposedDropdownMenuBoxScope.ExposedDropdownMenu
//  -- Modified for purpose by using a LazyColumn instead of Column
//  -- Rationale: ExposedDropdownMenu has absolutely horrific performance otherwise
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExposedDropdownMenuBoxScope.ExposedLazyDropdownMenu(
	expanded: Boolean,
	items: List<T>,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	offset: DpOffset = DpOffset.Zero,
	scrollState: LazyListState = rememberLazyListState(),
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	content: @Composable (LazyItemScope.(Int, T) -> Unit)
) {
	val expandedState = remember { MutableTransitionState(false) }
	expandedState.targetState = expanded

	if (expandedState.currentState || expandedState.targetState) {
		val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
		val density = LocalDensity.current
		val popupPositionProvider = remember(density) {
			ScrollableDropdownMenuPositionProvider(
				offset,
				density,
			) { anchorBounds, menuBounds ->
				transformOriginState.value = calculateTransformOrigin(anchorBounds, menuBounds)
			}
		}

		ExposedDropdownMenuPopup(
			onDismissRequest = onDismissRequest,
			popupPositionProvider = popupPositionProvider
		) {
			ScrollableDropdownMenuContent(
				expandedStates = expandedState,
				transformOriginState = transformOriginState,
				state = scrollState,
				modifier = modifier.exposedDropdownSize(),
				items = items,
				verticalArrangement = verticalArrangement,
				content = content
			)
		}
	}
}

// modeled after DropdownMenuContent but uses LazyColumn instead of a scrollable column
@Composable
private fun <T> ScrollableDropdownMenuContent(
    expandedStates: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    state: LazyListState,
    items: List<T>,
	verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable (LazyItemScope.(Int, T) -> Unit)
) {
    // Menu open/close animation.
    val transition = updateTransition(expandedStates, "DropDownMenu")
    val inTransitionDuration = 120
    val outTransitionDuration = 75
    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(
                    durationMillis = inTransitionDuration,
                    easing = LinearOutSlowInEasing
                )
            } else {
                // Expanded to dismissed.
                tween(
                    durationMillis = 1,
                    delayMillis = outTransitionDuration - 1
                )
            }
        }, label = ""
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0.8f
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(durationMillis = 30)
            } else {
                // Expanded to dismissed.
                tween(durationMillis = outTransitionDuration)
            }
        }, label = ""
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0f
        }
    }
    Card(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            transformOrigin = transformOriginState.value
        },
    ) {
        LazyColumn(
            state = state,
			verticalArrangement = verticalArrangement,
            modifier = modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, item ->
                content(index, item)
            }
        }
    }
}

private data class ScrollableDropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    private val menuVerticalMargin = 48.dp
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { menuVerticalMargin.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(
                toRight,
                toLeft,
                // If the anchor gets outside of the window on the left, we want to position
                // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
            )
        } else {
            sequenceOf(
                toLeft,
                toRight,
                // If the anchor gets outside of the window on the right, we want to position
                // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
            )
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
            it >= verticalMargin &&
                it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}

private fun calculateTransformOrigin(
    parentBounds: IntRect,
    menuBounds: IntRect
): TransformOrigin {
    val pivotX = when {
        menuBounds.left >= parentBounds.right -> 0f
        menuBounds.right <= parentBounds.left -> 1f
        menuBounds.width == 0 -> 0f
        else -> {
            val intersectionCenter =
                (
                    max(parentBounds.left, menuBounds.left) +
                        min(parentBounds.right, menuBounds.right)
                    ) / 2
            (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
        }
    }
    val pivotY = when {
        menuBounds.top >= parentBounds.bottom -> 0f
        menuBounds.bottom <= parentBounds.top -> 1f
        menuBounds.height == 0 -> 0f
        else -> {
            val intersectionCenter =
                (
                    max(parentBounds.top, menuBounds.top) +
                        min(parentBounds.bottom, menuBounds.bottom)
                    ) / 2
            (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
        }
    }
    return TransformOrigin(pivotX, pivotY)
}

/**
 * Popup specific for exposed dropdown menus. Should not be used in other components.
 * See b/202810604.
 */
@Composable
private fun ExposedDropdownMenuPopup(
    onDismissRequest: (() -> Unit)? = null,
    popupPositionProvider: PopupPositionProvider,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val popupId = rememberSaveable { UUID.randomUUID() }
    val popupLayout = remember {
        PopupLayout(
            onDismissRequest = onDismissRequest,
            composeView = view,
            positionProvider = popupPositionProvider,
            density = density,
            popupId = popupId
        ).apply {
            setContent(parentComposition) {
                SimpleStack(
                    Modifier
                        .semantics { this.popup() }
                        // Get the size of the content
                        .onSizeChanged {
                            popupContentSize = it
                            updatePosition()
                        }
                        // Hide the popup while we can't position it correctly
                        .alpha(if (canCalculatePosition) 1f else 0f)
                ) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(popupLayout) {
        popupLayout.show()
        popupLayout.updateParameters(
            onDismissRequest = onDismissRequest,
            layoutDirection = layoutDirection
        )
        onDispose {
            popupLayout.disposeComposition()
            // Remove the window
            popupLayout.dismiss()
        }
    }

    SideEffect {
        popupLayout.updateParameters(
            onDismissRequest = onDismissRequest,
            layoutDirection = layoutDirection
        )
    }

    // TODO(soboleva): Look at module arrangement so that Box can be
    //  used instead of this custom Layout
    // Get the parent's position, size and layout direction
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentLayoutCoordinates!!
            val layoutSize = coordinates.size

            val position = coordinates.positionInWindow()
            val layoutPosition = IntOffset(position.x.roundToInt(), position.y.roundToInt())

            popupLayout.parentBounds = IntRect(layoutPosition, layoutSize)
            // Update the popup's position
            popupLayout.updatePosition()
        }
    ) { _, _ ->
        popupLayout.parentLayoutDirection = layoutDirection
        layout(0, 0) {}
    }
}

// TODO(soboleva): Look at module dependencies so that we can get code reuse between
// Popup's SimpleStack and Box.
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun SimpleStack(modifier: Modifier, noinline content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        when (measurables.size) {
            0 -> layout(0, 0) {}
            1 -> {
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.placeRelative(0, 0)
                }
            }
            else -> {
                val placeables = measurables.fastMap { it.measure(constraints) }
                var width = 0
                var height = 0
                for (i in 0..placeables.lastIndex) {
                    val p = placeables[i]
                    width = maxOf(width, p.width)
                    height = maxOf(height, p.height)
                }
                layout(width, height) {
                    for (i in 0..placeables.lastIndex) {
                        val p = placeables[i]
                        p.placeRelative(0, 0)
                    }
                }
            }
        }
    }
}

/**
 * The layout the popup uses to display its content.
 *
 * @param composeView The parent view of the popup which is the AndroidComposeView.
 */
@SuppressLint("ViewConstructor")
private class PopupLayout(
    private var onDismissRequest: (() -> Unit)?,
    private val composeView: View,
    private val positionProvider: PopupPositionProvider,
    density: Density,
    popupId: UUID
) : AbstractComposeView(composeView.context),
    ViewRootForInspector,
    ViewTreeObserver.OnGlobalLayoutListener {

    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val params = createLayoutParams()

    // Position params
    var parentLayoutDirection: LayoutDirection = LayoutDirection.Ltr
    var parentBounds: IntRect? by mutableStateOf(null)
    var popupContentSize: IntSize? by mutableStateOf(null)

    // Track parent bounds and content size; only show popup once we have both
    val canCalculatePosition by derivedStateOf { parentBounds != null && popupContentSize != null }

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp

    // The window visible frame used for the last popup position calculation.
    private val previousWindowVisibleFrame = ViewRect()
    private val tmpWindowVisibleFrame = ViewRect()

    override val subCompositionView: AbstractComposeView get() = this

    init {
        id = android.R.id.content
        setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        composeView.viewTreeObserver.addOnGlobalLayoutListener(this)
        // Set unique id for AbstractComposeView. This allows state restoration for the state
        // defined inside the Popup via rememberSaveable()
        setTag(R.id.compose_view_saveable_id_tag, "Popup:$popupId")

        // Enable children to draw their shadow by not clipping them
        clipChildren = false
        // Allocate space for elevation
        with(density) { elevation = maxSupportedElevation.toPx() }
        // Simple outline to force window manager to allocate space for shadow.
        // Note that the outline affects clickable area for the dismiss listener. In case of shapes
        // like circle the area for dismiss might be to small (rectangular outline consuming clicks
        // outside of the circle).
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, result: Outline) {
                result.setRect(0, 0, view.width, view.height)
                // We set alpha to 0 to hide the view's shadow and let the composable to draw its
                // own shadow. This still enables us to get the extra space needed in the surface.
                result.alpha = 0f
            }
        }
    }

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun show() {
        windowManager.addView(this, params)
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    @Composable
    override fun Content() {
        content()
    }

    // Taken from PopupWindow. Calls [onDismissRequest] when back button is pressed.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (keyDispatcherState == null) {
                return super.dispatchKeyEvent(event)
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val state = keyDispatcherState
                state?.startTracking(event, this)
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val state = keyDispatcherState
                if (state != null && state.isTracking(event) && !event.isCanceled) {
                    onDismissRequest?.invoke()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    fun updateParameters(
        onDismissRequest: (() -> Unit)?,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        superSetLayoutDirection(layoutDirection)
    }

    /**
     * Updates the position of the popup based on current position properties.
     */
    fun updatePosition() {
        val parentBounds = parentBounds ?: return
        val popupContentSize = popupContentSize ?: return

        val windowSize = previousWindowVisibleFrame.let {
            composeView.getWindowVisibleDisplayFrame(it)
            val bounds = it.toIntBounds()
            IntSize(width = bounds.width, height = bounds.height)
        }

        val popupPosition = positionProvider.calculatePosition(
            parentBounds,
            windowSize,
            parentLayoutDirection,
            popupContentSize
        )

        params.x = popupPosition.x
        params.y = popupPosition.y

        windowManager.updateViewLayout(this, params)
    }

    /**
     * Remove the view from the [WindowManager].
     */
    fun dismiss() {
        setViewTreeLifecycleOwner(null)
        composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        windowManager.removeViewImmediate(this)
    }

    /**
     * Handles touch screen motion events and calls [onDismissRequest] when the
     * users clicks outside the popup.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)

        // Note that this implementation is taken from PopupWindow. It actually does not seem to
        // matter whether we return true or false as some upper layer decides on whether the
        // event is propagated to other windows or not. So for focusable the event is consumed but
        // for not focusable it is propagated to other windows.
        if (event.action == MotionEvent.ACTION_OUTSIDE ||
            (event.action == MotionEvent.ACTION_DOWN &&
                (event.x < 0 || event.x >= width || event.y < 0 || event.y >= height))
        ) {
            val parentBounds = parentBounds
            val shouldDismiss = parentBounds == null || dismissOnOutsideClick(
                // Keep menu open if ACTION_OUTSIDE event is reported as raw coordinates of (0, 0).
                // This means it belongs to another owner, e.g., the soft keyboard or other window.
                if (event.rawX != 0f && event.rawY != 0f) Offset(event.rawX, event.rawY) else null,
                parentBounds
            )

            if (shouldDismiss) {
                onDismissRequest?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        // Do nothing. ViewRootImpl will call this method attempting to set the layout direction
        // from the context's locale, but we have one already from the parent composition.
    }

    // Sets the "real" layout direction for our content that we obtain from the parent composition.
    private fun superSetLayoutDirection(layoutDirection: LayoutDirection) {
        val direction = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
        super.setLayoutDirection(direction)
    }

    // Specific to exposed dropdown menus.
    private fun dismissOnOutsideClick(offset: Offset?, bounds: IntRect): Boolean =
        if (offset == null) {
            false
        } else {
            offset.x < bounds.left || offset.x > bounds.right ||
                offset.y < bounds.top || offset.y > bounds.bottom
        }

    /**
     * Initialize the LayoutParams specific to [android.widget.PopupWindow].
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            // Start to position the popup in the top left corner, a new position will be calculated
            gravity = Gravity.START or Gravity.TOP

            // Flags specific to exposed dropdown menu.
            flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED

            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

            // Get the Window token from the parent view
            token = composeView.applicationWindowToken

            // Wrap the frame layout which contains composable content
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT

            format = PixelFormat.TRANSLUCENT

            // accessibilityTitle is not exposed as a public API therefore we set popup window
            // title which is used as a fallback by a11y services
            title = composeView.context.resources.getString(R.string.default_popup_window_title)
        }
    }

    private fun ViewRect.toIntBounds() = IntRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )

    override fun onGlobalLayout() {
        // Update the position of the popup, in case getWindowVisibleDisplayFrame has changed.
        composeView.getWindowVisibleDisplayFrame(tmpWindowVisibleFrame)
        if (tmpWindowVisibleFrame != previousWindowVisibleFrame) {
            updatePosition()
        }
    }
}

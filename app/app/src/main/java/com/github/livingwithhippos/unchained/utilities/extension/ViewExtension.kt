package com.github.livingwithhippos.unchained.utilities.extension

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.ScaleDrawable
import android.graphics.drawable.VectorDrawable
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.AttrRes
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.repository.model.PluginStatus
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay

/**
 * tint the color of a [ProgressBar] layer drawable
 *
 * @param layerId: the layer to tint
 * @param color: the color to use
 */
fun ProgressBar.tintDrawable(layerId: Int, color: Int) {
    val progressDrawable = getDrawableByLayerId(layerId).mutate()
    progressDrawable.setTint(color)
}

/**
 * set a [ProgressBar] certain layer's [Drawable]
 *
 * @param layerId: the layer to edit
 * @param drawable: the drawable to apply
 */
fun ProgressBar.swapLayerDrawable(layerId: Int, drawable: Drawable) {
    when (val oldDrawable = getDrawableByLayerId(layerId)) {
        is ClipDrawable -> oldDrawable.drawable = drawable
        is ScaleDrawable -> oldDrawable.drawable = drawable
        is InsetDrawable -> oldDrawable.drawable = drawable
        is RotateDrawable -> oldDrawable.drawable = drawable
        is VectorDrawable -> getLayerDrawable().setDrawableByLayerId(layerId, drawable)
    // ShapeDrawable is a generic shape and does not have drawables
    // is ShapeDrawable ->
    }
}

/**
 * get the [ProgressBar] main [LayerDrawable]
 *
 * @return the progress bar LayerDrawable
 */
fun ProgressBar.getLayerDrawable(): LayerDrawable {
    return (if (isIndeterminate) indeterminateDrawable else progressDrawable) as LayerDrawable
}

/**
 * get the [ProgressBar] Drawable with a certain id
 *
 * @param id: the id of the layer
 * @return the progress bar layer Drawable
 */
fun ProgressBar.getDrawableByLayerId(id: Int): Drawable {
    return getLayerDrawable().findDrawableByLayerId(id)
}

fun getFileSizeString(context: Context, size: Long): String {
    return when {
        size < 1048575 -> context.getString(R.string.file_size_format_kb, size.toFloat() / 1024)
        size < 1073741823 ->
            context.getString(R.string.file_size_format_mb, size.toFloat() / 1024 / 1024)
        // ~9 TB, for now it's more probable that a wrong value is being passed if it's over
        // this
        // value
        size < 9999999999999 ->
            context.getString(R.string.file_size_format_gb, size.toFloat() / 1024 / 1024 / 1024)
        // todo: shorten this
        else -> context.getString(R.string.size_error)
    }
}

/**
 * Simulate a ripple animation on a [View]
 *
 * @param delay: the delay after which the animation is started
 */
fun View.runRippleAnimation(delay: Long = 300) {
    // todo: test if this works for things beside buttons
    if (background is RippleDrawable) {
        postDelayed(
            {
                background.state =
                    intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
            },
            delay,
        )
    }
}

/**
 * Smoothly scrolls to an item position in a RecyclerView.
 *
 * @param context: The Context to create a SmoothScroller
 * @param position: the position to scroll to
 * @param snapType: how to align the child view with parent view
 */
fun RecyclerView.LayoutManager.verticalScrollToPosition(
    context: Context,
    position: Int = 0,
    snapType: Int = LinearSmoothScroller.SNAP_TO_START,
) {

    val smoothScroller =
        object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference(): Int {
                    return snapType
                }
            }
            .apply<LinearSmoothScroller> { targetPosition = position }

    this.startSmoothScroll(smoothScroller)
}

/**
 * Scrolls to the top of a list after a delay The delay is needed if scrolling after updating the
 * list. It probably depends on the device.
 *
 * @param context
 * @param delay
 */
suspend fun RecyclerView.delayedScrolling(context: Context, delay: Long = 300) {
    this.layoutManager?.let {
        delay(delay)
        it.verticalScrollToPosition(context)
    }
}

/**
 * Shows a SnackBar
 *
 * @param messageResource: the resource ID of the string to be displayed
 * @param action: an optional function to be executed as action. Supports only the Unit return type.
 * @param actionText String resource to display for the action
 * @param anchor: the View where the snackbar will be anchored to
 * @param length How long to display the message. Either Snackbar.LENGTH_SHORT,
 *   Snackbar.LENGTH_LONG, or Snackbar.LENGTH_INDEFINITE. Defaults to LENGTH_SHORT
 */
fun View.showSnackBar(
    messageResource: Int,
    length: Int = Snackbar.LENGTH_SHORT,
    action: (() -> Unit)? = null,
    actionText: Int? = null,
    anchor: View? = null,
) {
    Snackbar.make(this, messageResource, length)
        .also {
            if (anchor != null) it.anchorView = anchor
            if (action != null && actionText != null) it.setAction(actionText) { action() }
        }
        .show()
}

fun getThemeColor(context: Context, @AttrRes resId: Int): Int {
    // get a reference to the current theme
    val typedValue = TypedValue()
    val theme: Resources.Theme = context.theme
    theme.resolveAttribute(resId, typedValue, true)
    return typedValue.data
}

fun setDrawableByPluginStatus(view: ImageView, status: String, disabled: Boolean = false) {
    if (disabled) return view.setImageResource(R.drawable.icon_close)
    return when (status) {
        PluginStatus.updated -> view.setImageResource(R.drawable.icon_check)
        PluginStatus.hasUpdate -> view.setImageResource(R.drawable.icon_reload)
        PluginStatus.hasIncompatibleUpdate -> view.setImageResource(R.drawable.icon_close)
        PluginStatus.isNew -> view.setImageResource(R.drawable.icon_new_releases)
        PluginStatus.incompatible -> view.setImageResource(R.drawable.icon_close)
        PluginStatus.unknown -> view.setImageResource(R.drawable.icon_question_mark)
        PluginStatus.disabled -> view.setImageResource(R.drawable.icon_close)
        else -> view.setImageResource(R.drawable.icon_question_mark)
    }
}

fun setDrawableByServiceType(view: ImageView, type: Int) {
    return when (type) {
        RemoteServiceType.KODI.value -> view.setImageResource(R.drawable.icon_kodi)
        RemoteServiceType.VLC.value -> view.setImageResource(R.drawable.icon_vlc)
        RemoteServiceType.JACKETT.value -> view.setImageResource(R.drawable.icon_jackett)
        else -> view.setImageResource(R.drawable.icon_play_outline)
    }
}

/** hides the keyboard when called on a View */
fun View.hideKeyboard() {
    context?.let {
        val inputMethodManager =
            it.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(this.windowToken, 0)
    }
}

/**
 * This function returns the available space around a view Order is top, right, bottom, left
 * Multiscreen will return sizes against the screen, not the app window
 */
fun getAvailableSpace(view: View): List<Int> {
    val screenRect = Rect()
    val windowManager = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getRectSize(screenRect)

    val parentViewRect = Rect()
    val locationOnScreen = IntArray(2)

    view.getLocationOnScreen(locationOnScreen)
    view.getDrawingRect(parentViewRect)

    val leftSpace = locationOnScreen[0]
    val rightSpace = screenRect.width() - (leftSpace + parentViewRect.width())

    val topSpace = locationOnScreen[1]
    val bottomSpace = screenRect.height() - (topSpace + parentViewRect.width())

    return listOf(topSpace, rightSpace, bottomSpace, leftSpace)
}

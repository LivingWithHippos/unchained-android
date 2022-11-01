package com.github.livingwithhippos.unchained.utilities.extension

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Animatable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.ScaleDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.extensionIconMap
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay

/**
 * start an animation from the [ImageView] drawable if possible
 * @param start: true to start the animation or false to stop it
 */
@BindingAdapter("startAnimation")
fun ImageView.startAnimation(start: Boolean) {
    if (drawable is Animatable) {
        if (start)
            (drawable as Animatable).start()
        else
            (drawable as Animatable).stop()
    }
}

/**
 * set a simple [AutoCompleteTextView] items adapter
 * @param contents: the list to be show on the dropdown menu
 */
@BindingAdapter("adapter")
fun AutoCompleteTextView.setAdapter(contents: List<String>) {
    // a simple layout is set for the dropdown items
    val adapter = ArrayAdapter(this.context, R.layout.item_dropdown_plain, contents)
    this.setAdapter(adapter)
}

/**
 * set the background [ProgressBar] color
 * @param color: the color to be shown
 */
@BindingAdapter("backgroundProgressColor")
fun ProgressBar.setBackgroundProgressColor(color: Int) {
    tintDrawable(android.R.id.background, color)
}

/**
 * set the primary [ProgressBar] color
 * @param color: the color to be shown
 */
@BindingAdapter("progressColor")
fun ProgressBar.setProgressColor(color: Int) {
    tintDrawable(android.R.id.progress, color)
}

/**
 * set the secondary [ProgressBar] color
 * @param color: the color to be shown
 */
@BindingAdapter("secondaryProgressColor")
fun ProgressBar.setSecondaryProgressColor(color: Int) {
    tintDrawable(android.R.id.secondaryProgress, color)
}

/**
 * set the background progress drawable for the [ProgressBar]
 * @param drawable: the drawable for the background
 */
@BindingAdapter("backgroundProgressDrawable")
fun ProgressBar.setBackgroundProgressDrawable(drawable: Drawable) {
    swapLayerDrawable(android.R.id.background, drawable)
}

/**
 * set the primary progress drawable for the [ProgressBar]
 * @param drawable: the drawable
 */
@BindingAdapter("primaryProgressDrawable")
fun ProgressBar.setPrimaryProgressDrawable(drawable: Drawable) {
    swapLayerDrawable(android.R.id.progress, drawable)
}

/**
 * set the secondary progress drawable for the [ProgressBar]
 * @param drawable: the drawable
 */
@BindingAdapter("secondaryProgressDrawable")
fun ProgressBar.setSecondaryProgressDrawable(drawable: Drawable) {
    swapLayerDrawable(android.R.id.secondaryProgress, drawable)
}

/**
 * tint the color of a [ProgressBar] layer drawable
 * @param layerId: the layer to tint
 * @param color: the color to use
 */
fun ProgressBar.tintDrawable(layerId: Int, color: Int) {
    val progressDrawable = getDrawableByLayerId(layerId).mutate()
    progressDrawable.setTint(color)
}

/**
 * set a [ProgressBar] certain layer's [Drawable]
 * @param layerId: the layer to edit
 * @param drawable: the drawable to apply
 */
fun ProgressBar.swapLayerDrawable(layerId: Int, drawable: Drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
}

/**
 * get the [ProgressBar] main [LayerDrawable]
 * @return the progress bar LayerDrawable
 */
fun ProgressBar.getLayerDrawable(): LayerDrawable {
    return (if (isIndeterminate) indeterminateDrawable else progressDrawable) as LayerDrawable
}

/**
 * get the [ProgressBar] Drawable with a certain id
 * @param id: the id of the layer
 * @return the progress bar layer Drawable
 */
fun ProgressBar.getDrawableByLayerId(id: Int): Drawable {
    return getLayerDrawable().findDrawableByLayerId(id)
}

/**
 * set the [ProgressIndicator] progress value, not available as xml tag
 * @param progress: the progress to be set
 */
@BindingAdapter("progressCompat")
fun BaseProgressIndicator<*>.setRealProgress(progress: Int) {
    val animated = true
    this.setProgressCompat(progress, animated)
}

/**
 * This function format the download speed from bytes/s to kb/s and MB/s and assign it to the [TextView]
 * @param speed - the speed in bytes/s.
 */
@BindingAdapter("downloadSpeed")
fun TextView.setDownloadSpeed(speed: Int) {
    this.text = when (speed.toString().length) {
        in 0..3 -> this.context.getString(R.string.speed_format_b, speed)
        in 4..6 -> this.context.getString(R.string.speed_format_kb, speed.toFloat() / 1000)
        in 7..15 -> this.context.getString(R.string.speed_format_mb, speed.toFloat() / 1000000)
        else -> this.context.getString(R.string.speed_error)
    }
}

/**
 * This function format the file size from bytes to Kb, Mb, Gb and assign it to the [TextView]
 * @param size - the file size in bytes.
 */
@BindingAdapter("fileSize")
fun TextView.setFileSize(size: Long) {
    this.text = when {
        size < 1048575 -> this.context.getString(
            R.string.file_size_format_kb,
            size.toFloat() / 1024
        )
        size < 1073741823 -> this.context.getString(
            R.string.file_size_format_mb,
            size.toFloat() / 1024 / 1024
        )
        // ~9 TB, for now it's more probable that a wrong value is being passed if it's over this value
        size < 9999999999999 -> this.context.getString(
            R.string.file_size_format_gb,
            size.toFloat() / 1024 / 1024 / 1024
        )
        // todo: shorten this
        else -> this.context.getString(R.string.size_error)
    }
}

/**
 * This function sets a SpannableStringBuilder as the TextView text.
 * @param spannableStringBuilder - the text to be displayed
 */
@BindingAdapter("spannableText")
fun TextView.setTextFromSpan(spannableStringBuilder: SpannableStringBuilder?) {
    if (spannableStringBuilder != null)
        setText(spannableStringBuilder, TextView.BufferType.SPANNABLE)
}

/**
 * Simulate a ripple animation on a [View]
 * @param delay: the delay after which the animation is started
 */
fun View.runRippleAnimation(delay: Long = 300) {
    // todo: test if this works for things beside buttons
    if (background is RippleDrawable) {
        postDelayed(
            {
                background.state = intArrayOf(
                    android.R.attr.state_pressed,
                    android.R.attr.state_enabled
                )
            },
            delay
        )
    }
}

/**
 * Smoothly scrolls to an item position in a RecyclerView.
 * @param context: The Context to create a SmoothScroller
 * @param position: the position to scroll to
 * @param snapType: how to align the child view with parent view
 */
fun RecyclerView.LayoutManager.verticalScrollToPosition(
    context: Context,
    position: Int = 0,
    snapType: Int = LinearSmoothScroller.SNAP_TO_START
) {

    val smoothScroller = object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int {
            return snapType
        }
    }.apply<LinearSmoothScroller> { targetPosition = position }

    this.startSmoothScroll(smoothScroller)
}

/**
 * Scrolls to the top of a list after a delay
 * The delay is needed if scrolling after updating the list. It probably depends on the device.
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
 * @param messageResource: the resource ID of the string to be displayed
 * @param action: an optional function to be executed as action. Supports only the Unit return type.
 * @param actionText String resource to display for the action
 * @param anchor: the View where the snackbar will be anchored to
 * @param length How long to display the message.  Either Snackbar.LENGTH_SHORT,
 *                Snackbar.LENGTH_LONG, or Snackbar.LENGTH_INDEFINITE. Defaults to LENGTH_SHORT
 */
fun View.showSnackBar(
    messageResource: Int,
    length: Int = Snackbar.LENGTH_SHORT,
    action: (() -> Unit)? = null,
    actionText: Int? = null,
    anchor: View? = null
) {
    Snackbar.make(this, messageResource, length)
        .also {
            if (anchor != null)
                it.anchorView = anchor
            if (action != null && actionText != null)
                it.setAction(actionText) { action() }
        }
        .show()
}

/**
 * The refresh indicator is not themed according to the app, it's always a black arrow in a white circle.
 * This can be used to paint it.
 */
@BindingAdapter("refreshColorTheme")
fun SwipeRefreshLayout.setRefreshThemeColor(themed: Boolean) {
    if (themed) {
        // get a reference to the current theme
        val typedValue = TypedValue()
        val theme: Resources.Theme = context.theme
        theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
        // arrow color
        val arrowColor = typedValue.data
        // this function accept a number of colors, the refresh indicator will rotate between them.
        setColorSchemeColors(arrowColor)
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        // background color
        val backgroundColor = typedValue.data
        setProgressBackgroundColorSchemeColor(backgroundColor)
    }
}

@BindingAdapter("mapDrawable")
fun ImageView.setDrawableByExtension(fileName: String) {
    val extension = fileName.substringAfterLast(".").lowercase()
    if (extensionIconMap.containsKey(extension))
        this.setImageResource(extensionIconMap.getValue(extension))
    else
        this.setImageResource(extensionIconMap.getValue("default"))
}

/**
 * hides the keyboard when called on a View
 *
 */
fun View.hideKeyboard() {
    context?.let {
        val inputMethodManager =
            it.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(this.windowToken, 0)
    }
}

package com.github.livingwithhippos.unchained.utilities.extension

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.ScaleDrawable
import android.graphics.drawable.VectorDrawable
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.google.android.material.progressindicator.ProgressIndicator


@BindingAdapter("startAnimation")
fun ImageView.startAnimation(start: Boolean) {
    if (drawable is Animatable) {
        if (start)
            (drawable as Animatable).start()
        else
            (drawable as Animatable).stop()
    }
}


@BindingAdapter("adapter")
fun AutoCompleteTextView.setAdapter(contents: List<String>) {
    // a simple layout is set for the dropdown items
    val adapter = ArrayAdapter<String>(this.context, R.layout.dropdown_plain_item, contents)
    this.setAdapter(adapter)
}

@BindingAdapter("backgroundProgressColor")
fun ProgressBar.setBackgroundProgressColor(color: Int) {
    tintDrawable(android.R.id.background, color)
}

@BindingAdapter("progressColor")
fun ProgressBar.setProgressColor(color: Int) {
    tintDrawable(android.R.id.progress, color)
}

@BindingAdapter("secondaryProgressColor")
fun ProgressBar.setSecondaryProgressColor(color: Int) {
    tintDrawable(android.R.id.secondaryProgress, color)
}

@BindingAdapter("backgroundProgressDrawable")
fun ProgressBar.setBackgroundProgressDrawable(drawable: Drawable) {
    swapLayerDrawable(android.R.id.background, drawable)
}

@BindingAdapter("primaryProgressDrawable")
fun ProgressBar.setPrimaryProgressDrawable(drawable: Drawable) {
    swapLayerDrawable(android.R.id.progress, drawable)
}

@BindingAdapter("secondaryProgressDrawable")
fun ProgressBar.setSecondaryProgressDrawable(drawable: Drawable) {
    swapLayerDrawable(android.R.id.secondaryProgress, drawable)
}

fun ProgressBar.tintDrawable(layerId: Int, color: Int) {
    val progressDrawable = getDrawableByLayerId(layerId).mutate()
    progressDrawable.setTint(color)
}

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

fun ProgressBar.getLayerDrawable(): LayerDrawable {
    return (if (isIndeterminate) indeterminateDrawable else progressDrawable) as LayerDrawable
}

fun ProgressBar.getDrawableByLayerId(id: Int): Drawable {
    return getLayerDrawable().findDrawableByLayerId(id)
}

@BindingAdapter("progressCompat")
fun ProgressIndicator.setRealProgress(progress: Int) {
    val animated: Boolean = true
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
        size < 1023 -> this.context.getString(R.string.file_size_format_b, size)
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
        else -> this.context.getString(R.string.size_error)
    }
}

fun View.runRippleAnimation(delay: Long = 300) {
    //todo: test
    if (background is RippleDrawable) {
        postDelayed(
            Runnable {
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

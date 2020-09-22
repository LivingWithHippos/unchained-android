package com.github.livingwithhippos.unchained.utilities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.ScaleDrawable
import android.graphics.drawable.VectorDrawable
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.github.livingwithhippos.unchained.R
import com.google.android.material.progressindicator.ProgressIndicator
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

//todo: split extensions in own files (glide/views etc)
@BindingAdapter("imageURL")
fun ImageView.loadImage(imageURL: String?) {
    if (imageURL != null)
        GlideApp.with(this.context)
            .load(imageURL)
            .into(this)
}

@BindingAdapter("startAnimation")
fun ImageView.startAnimation(start: Boolean) {
    if (drawable is Animatable) {
        if (start)
            (drawable as Animatable).start()
        else
            (drawable as Animatable).stop()
    }
}

@BindingAdapter("blurredBackground")
fun ConstraintLayout.blurredBackground(drawable: Drawable) {
    val layout = this
    GlideApp.with(this)
        .load(drawable)
        .apply(bitmapTransform(BlurTransformation(context)))
        .into(object : CustomViewTarget<ConstraintLayout, Drawable>(layout) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                // error handling
            }

            override fun onResourceCleared(placeholder: Drawable?) {
                // clear all resources
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                layout.background = resource
            }
        })
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
        in 4..6 -> this.context.getString(R.string.speed_format_kb, speed.toFloat()/1000)
        in 7..15 -> this.context.getString(R.string.speed_format_mb, speed.toFloat()/1000000)
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
        size < 1048575 -> this.context.getString(R.string.file_size_format_kb, size.toFloat()/1024)
        size < 1073741823 -> this.context.getString(R.string.file_size_format_mb, size.toFloat()/ 1024/ 1024)
        // ~9 TB, for now it's more probable that a wrong value is being passed if it's over this value
        size < 9999999999999 -> this.context.getString(R.string.file_size_format_gb, size.toFloat()/ 1024/ 1024/ 1024)
        else -> this.context.getString(R.string.size_error)
    }
}

fun View.runRippleAnimation(delay: Long=300){
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

fun Fragment.showToast(stringResource: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), getString(stringResource), length).show()
}

// note: should these be added to Context instead of Fragment?
fun Fragment.copyToClipboard(label: String, text: String) {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip: ClipData = ClipData.newPlainText(label, text)
    // Set the clipboard's primary clip.
    clipboard.setPrimaryClip(clip)
}

fun Fragment.getClipboardText(): String {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var text = ""
    if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(
            MIMETYPE_TEXT_PLAIN
        ) == true) {
        val item = clipboard.primaryClip!!.getItemAt(0)
        text = item.text.toString()
    } else {
        Log.d(
            "Fragment.getClipboardText",
            "Clipboard was empty or did not contain any text mime type."
        )
    }
    return text
}

fun Fragment.openExternalWebPage(url: String, showErrorToast: Boolean = true): Boolean {
    // this pattern accepts everything that is something.tld since there were too many new tlds and Google gave up updating their regex
    if (url.isWebUrl()) {
        val webIntent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(webIntent)
        return true
    } else
        if (showErrorToast)
            showToast(R.string.invalid_url)

    return false
}

fun String.isWebUrl(): Boolean =
    Patterns.WEB_URL.matcher(this).matches()

fun String.isMagnet(): Boolean {
    val m: Matcher = Pattern.compile(MAGNET_PATTERN).matcher(this)
    return m.lookingAt()
}

/**
 * this function can be used to create a new context with a particular locale.
 * It must be used while overriding Activity.attachBaseContext like this:
  override fun attachBaseContext(base: Context?) {
        if (base != null)
            super.attachBaseContext(getUpdatedLocaleContext(base, "en"))
        else
            super.attachBaseContext(null)
    }
 * it must be applied to all the activities or added to a BaseActivity extended by them
 */
fun Activity.getUpdatedLocaleContext(context: Context, language: String): Context {
    val locale: Locale = Locale(language)
    val configuration: Configuration = Configuration(context.resources.configuration)
    // check if this is necessary
    Locale.setDefault(locale)
    configuration.setLocale(locale)
    return context.createConfigurationContext(configuration)
}

@SuppressLint("SimpleDateFormat")
fun stringToDate(rdDate: String): String {
    val originalDate: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
    val date: Date = originalDate.parse(rdDate)
    val localDate: DateFormat = SimpleDateFormat.getDateTimeInstance()
    return localDate.format(date)
}

package com.github.livingwithhippos.unchained.utilities.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.R
import java.util.*


/**
 * Show a toast message
 * @param stringResource: the string resource to be retrieved and shown
 * @param length How long to display the message.  Either {@link #LENGTH_SHORT} or
 *                 {@link #LENGTH_LONG} Defaults to short
 */
fun Context.showToast(stringResource: Int, length: Int = Toast.LENGTH_SHORT) =
    this.showToast(getString(stringResource, length))

/**
 * Show a toast message
 * @param message: the message and shown
 * @param length: the duration of the toast. Defaults to short
 */
fun Context.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

/**
 * Return the int value of the color of a certain attribute for the current theme
 * @param attributeID: the attribute id, like R.attr.colorAccent
 * @return the int value of the color
 */
fun Context.getThemeColor(attributeID: Int): Int {
    // get a reference to the current theme
    val typedValue = TypedValue()
    val theme: Resources.Theme = this.theme
    theme.resolveAttribute(attributeID, typedValue, true)
    return typedValue.data
}

/**
 * Copy some text on the clipboard
 * @param label: the label of the text copied
 * @param text: the text to be copied to clipboard
 */
fun Fragment.copyToClipboard(label: String, text: String) {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip: ClipData = ClipData.newPlainText(label, text)
    // Set the clipboard's primary clip.
    clipboard.setPrimaryClip(clip)
}

/**
 * Get the text from the clipboard
 * @return the text on the clipboard or "" if empty or not text
 */
fun Fragment.getClipboardText(): String {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var text = ""
    if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(
            MIMETYPE_TEXT_PLAIN
        ) == true
    ) {
        val item = clipboard.primaryClip!!.getItemAt(0)
        text = item.text.toString()
    } else {
        if (BuildConfig.DEBUG)
            Log.d(
                "Fragment.getClipboardText",
                "Clipboard was empty or did not contain any text mime type."
            )
    }
    return text
}

/**
 * Open an url from available apps
 * @param url: the url to be opened
 * @param showErrorToast: set to true if an error toast should be displayed
 * @return true if the passed url is correct, false otherwise
 */
fun Fragment.openExternalWebPage(url: String, showErrorToast: Boolean = true): Boolean {
    // this pattern accepts everything that is something.tld since there were too many new tlds and Google gave up updating their regex
    if (url.isWebUrl()) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(webIntent)
        return true
    } else
        if (showErrorToast)
            context?.showToast(R.string.invalid_url)


    return false
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
    val locale = Locale(language)
    val configuration = Configuration(context.resources.configuration)
    // check if this is necessary
    Locale.setDefault(locale)
    configuration.setLocale(locale)
    return context.createConfigurationContext(configuration)
}

fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}

fun <T, K> zipLiveData(t: LiveData<T>, k: LiveData<K>): LiveData<Pair<T, K>> {
    return MediatorLiveData<Pair<T, K>>().apply {
        var lastT: T? = null
        var lastK: K? = null

        fun update() {
            val localLastT = lastT
            val localLastK = lastK
            if (localLastT != null && localLastK != null)
                this.value = Pair(localLastT, localLastK)
        }

        addSource(t) {
            lastT = it
            update()
        }
        addSource(k) {
            lastK = it
            update()
        }
    }
}

fun <T> LiveData<T>.observeOnce(
    lifecycleOwner: LifecycleOwner,
    observer: Observer<T>,
    untilNotNull: Boolean = false
) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            if (untilNotNull) {
                if (t != null)
                    removeObserver(this)
            } else
                removeObserver(this)
        }
    })
}

fun AppCompatActivity.setCustomTheme(theme: String) {
    when (theme) {
        "original" -> setTheme(R.style.Theme_Unchained)
        "tropical_sunset" -> setTheme(R.style.Theme_TropicalSunset)
        "black_n_white" -> setTheme(R.style.Theme_BlackAndWhite)
    }
}

fun AppCompatActivity.setNavigationBarColor(color: Int, alpha: Int = 0) {
    val newColor = Color.argb(
        alpha, Color.red(color), Color.green(color), Color.blue(
            color
        )
    )

    // set the color before applying the light bar effect
    window.navigationBarColor = newColor

    val luminance = Color.luminance(color)
    if (luminance >= 0.25) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
            Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.Q -> {
                // the check above is not recognized
                @Suppress("DEPRECATION")
                @SuppressLint("InlinedApi")
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    } else
        @Suppress("DEPRECATION")
        @SuppressLint("InlinedApi")
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()

}

fun Context.getApiErrorMessage(errorCode: Int?): String {
    return when (errorCode) {
        -1 -> getString(R.string.internal_error)
        1 -> getString(R.string.missing_parameter)
        2 -> getString(R.string.bad_parameter_value)
        3 -> getString(R.string.unknown_method)
        4 -> getString(R.string.method_not_allowed)
        // note: what is this error for?
        5 -> getString(R.string.slow_down)
        6 -> getString(R.string.resource_unreachable)
        7 -> getString(R.string.resource_not_found)
        8 -> getString(R.string.bad_token)
        9 -> getString(R.string.permission_denied)
        10 -> getString(R.string.tfa_needed)
        11 -> getString(R.string.tfa_pending)
        12 -> getString(R.string.invalid_login)
        13 -> getString(R.string.invalid_password)
        14 -> getString(R.string.account_locked)
        15 -> getString(R.string.account_not_activated)
        16 -> getString(R.string.unsupported_hoster)
        17 -> getString(R.string.hoster_in_maintenance)
        18 -> getString(R.string.hoster_limit_reached)
        19 -> getString(R.string.hoster_temporarily_unavailable)
        20 -> getString(R.string.hoster_not_available_for_free_users)
        21 -> getString(R.string.too_many_active_downloads)
        22 -> getString(R.string.ip_Address_not_allowed)
        23 -> getString(R.string.traffic_exhausted)
        24 -> getString(R.string.file_unavailable)
        25 -> getString(R.string.service_unavailable)
        26 -> getString(R.string.upload_too_big)
        27 -> getString(R.string.upload_error)
        28 -> getString(R.string.file_not_allowed)
        29 -> getString(R.string.torrent_too_big)
        30 -> getString(R.string.torrent_file_invalid)
        31 -> getString(R.string.action_already_done)
        32 -> getString(R.string.image_resolution_error)
        33 -> getString(R.string.torrent_already_active)
        else -> getString(R.string.unknown_error)
    }
}


fun Context.getStatusTranslation(status: String): String {
    return when (status) {
        "magnet_error" -> getString(R.string.magnet_error)
        "magnet_conversion" -> getString(R.string.magnet_conversion)
        "waiting_files_selection" -> getString(R.string.waiting_files_selection)
        "queued" -> getString(R.string.queued)
        "downloading" -> getString(R.string.downloading)
        "downloaded" -> getString(R.string.downloaded)
        "error" -> getString(R.string.error)
        "virus" -> getString(R.string.virus)
        "compressing" -> getString(R.string.compressing)
        "uploading" -> getString(R.string.uploading)
        "dead" -> getString(R.string.dead)
        else -> getString(R.string.unknown_status)
    }
}

@Suppress("DEPRECATION")
fun Context.vibrate(duration: Long = 200) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        // minsdk is 24
        vibrator.vibrate(duration)
    }
}
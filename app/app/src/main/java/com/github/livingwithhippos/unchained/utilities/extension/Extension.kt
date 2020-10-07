package com.github.livingwithhippos.unchained.utilities.extension

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.SettingsFragment
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

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>, untilNotNull:  Boolean = false) {
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
        "tropical_sunset" -> setTheme(R.style.Theme_Unchained_TropicalSunset)
    }
}

fun Context.vibrate(duration: Long = 200){
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(duration)
    }
}
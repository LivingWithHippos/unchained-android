package com.github.livingwithhippos.unchained.utilities.extension

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.R
import java.util.*

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
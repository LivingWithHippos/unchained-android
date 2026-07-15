package com.github.livingwithhippos.unchained.utilities.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_HTML
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.view.CUSTOM_THEME_KEY
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_AUTO
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_DAY
import com.github.livingwithhippos.unchained.settings.view.ThemeItem
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.KEY_PREFERRED_VIDEO_PLAYER
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import com.github.livingwithhippos.unchained.utilities.VideoPlayerChosenReceiver
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.Locale
import timber.log.Timber

/**
 * Provides the list of available themes, used to easily get them with ids from anything with a
 * Context
 */
fun Context.getThemeList(): List<ThemeItem> {
    val staticThemes =
        listOf(
        ThemeItem(
            "Waves",
            "waves_01",
            THEME_DAY,
            R.style.Theme_Unchained_Material3_Waves_One,
            ResourcesCompat.getColor(resources, R.color.radical_red, null),
            ResourcesCompat.getColor(resources, R.color.waves_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.waves_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Black and White",
            "bnw_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_BnW_One,
            ResourcesCompat.getColor(resources, R.color.bnw_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.bnw_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.bnw_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Red",
            "red_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Red_One,
            ResourcesCompat.getColor(resources, R.color.red_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.red_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.red_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Orange",
            "orange_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Orange_One,
            ResourcesCompat.getColor(resources, R.color.orange_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.orange_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.orange_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Yellow",
            "yellow_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Yellow_One,
            ResourcesCompat.getColor(resources, R.color.yellow_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.yellow_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.yellow_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Purple",
            "purple_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Purple_One,
            ResourcesCompat.getColor(resources, R.color.purple_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.purple_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.purple_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Green",
            "green_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Green_One,
            ResourcesCompat.getColor(resources, R.color.green_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.green_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.green_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Blue",
            "blue_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Blue_One,
            ResourcesCompat.getColor(resources, R.color.blue_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.blue_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.blue_one_theme_primaryContainer, null),
        ),
        ThemeItem(
            "Grey",
            "grey_01",
            THEME_AUTO,
            R.style.Theme_Unchained_Material3_Grey_One,
            ResourcesCompat.getColor(resources, R.color.grey_one_theme_primary, null),
            ResourcesCompat.getColor(resources, R.color.grey_one_theme_surface, null),
            ResourcesCompat.getColor(resources, R.color.grey_one_theme_primaryContainer, null),
        ),
    )
    return if (DynamicColors.isDynamicColorAvailable()) {
        staticThemes + getDynamicWallpaperThemeItem() + getCustomThemeItem()
    } else {
        staticThemes
    }
}

/**
 * The "Material You" theme entry: colors come from the device wallpaper via Android's dynamic
 * color system instead of a fixed palette, so its preview swatch is only a best-effort
 * approximation of the current wallpaper colors, not the exact colors DynamicColors will apply
 */
private fun Context.getDynamicWallpaperThemeItem(): ThemeItem {
    val isNight =
        resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    // system_accent1/2 tonal ramps are always available once dynamic color itself is available
    val primaryTone = if (isNight) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
    val surfaceTone = if (isNight) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_10
    val containerTone = if (isNight) android.R.color.system_accent2_700 else android.R.color.system_accent2_100
    return ThemeItem(
        name = "Material You",
        key = "dynamic_wallpaper",
        nightMode = THEME_AUTO,
        themeID = R.style.Theme_Unchained_Material3_Dynamic,
        primaryColorID = ResourcesCompat.getColor(resources, primaryTone, null),
        surfaceColorID = ResourcesCompat.getColor(resources, surfaceTone, null),
        primaryContainerColorID = ResourcesCompat.getColor(resources, containerTone, null),
        isDynamic = true,
    )
}

/**
 * The "Custom" theme entry: colors are generated from a user-picked seed color instead of a
 * fixed palette or the wallpaper. The preview swatch approximates the generated palette by
 * blending the seed toward white, since the real palette only exists once DynamicColors applies
 * it to an actual activity.
 */
private fun Context.getCustomThemeItem(): ThemeItem {
    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    val seedColor =
        preferences.getInt(
            PreferenceKeys.Ui.CUSTOM_THEME_SEED_COLOR_KEY,
            ResourcesCompat.getColor(resources, R.color.green_one_theme_primary, null),
        )
    return ThemeItem(
        name = "Custom",
        key = CUSTOM_THEME_KEY,
        nightMode = THEME_AUTO,
        themeID = R.style.Theme_Unchained_Material3_DynamicCustom,
        primaryColorID = seedColor,
        surfaceColorID = ColorUtils.blendARGB(seedColor, Color.WHITE, 0.9f),
        primaryContainerColorID = ColorUtils.blendARGB(seedColor, Color.WHITE, 0.7f),
        isDynamic = true,
    )
}

/**
 * Show a toast message
 *
 * @param stringResource: the string resource to be retrieved and shown
 * @param length How long to display the message. Either {@link #LENGTH_SHORT} or
 *   {@link #LENGTH_LONG} Defaults to short
 */
fun Context.showToast(stringResource: Int, length: Int = Toast.LENGTH_SHORT) =
    this.showToast(getString(stringResource, length))

/**
 * Show a toast message
 *
 * @param message the message and shown
 * @param length the duration of the toast. Defaults to short
 */
fun Context.showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

/**
 * Return the int value of the color of a certain attribute for the current theme
 *
 * @param attributeID: the attribute id, like R.attr.colorAccent
 * @return the int value of the color
 */
fun Context.getThemeColor(@AttrRes attributeID: Int): Int {
    // get a reference to the current theme
    val typedValue = TypedValue()
    val theme: Resources.Theme = this.theme
    theme.resolveAttribute(attributeID, typedValue, true)
    return typedValue.data
}

/**
 * Sets the status and navigation bar icon colors (light or dark) to match the theme actually
 * applied to this activity right now, instead of relying on a static day/night assumption. Themes
 * don't necessarily get darker in night mode (none of them currently have night-specific colors)
 * and dynamic color themes aren't knowable ahead of time at all, so the only way to get readable
 * system icons for every theme, in every mode, is to check the real colors after they're applied.
 * See #315.
 */
fun Activity.applyThemeAwareSystemBarIconColors() {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.isAppearanceLightStatusBars =
        ColorUtils.calculateLuminance(getThemeColor(android.R.attr.colorPrimary)) > 0.5
    controller.isAppearanceLightNavigationBars =
        ColorUtils.calculateLuminance(getThemeColor(com.google.android.material.R.attr.colorSurface)) >
            0.5
}

/**
 * Returns a Drawable from its id with the tint color of the current theme
 *
 * @param id the Drawable id
 * @return the themed Drawable
 */
fun Context.getThemedDrawable(@DrawableRes id: Int): Drawable {
    return ResourcesCompat.getDrawable(resources, id, this.theme)
        ?: throw IllegalArgumentException("Drawable with id $id was missing")
}

// todo: verify if these can extend context and not fragment

/**
 * Copy some text on the clipboard
 *
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
 *
 * @return the text on the clipboard or "" if empty or not text
 */
fun Fragment.getClipboardText(): String {
    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var text = ""
    if (
        clipboard.hasPrimaryClip() &&
            (clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true ||
                clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_HTML) == true)
    ) {
        val item = clipboard.primaryClip!!.getItemAt(0)
        text = item.text.toString()
    } else {
        Timber.d(
            "Clipboard was empty or did not contain any text mime type: ${clipboard.primaryClipDescription}"
        )
    }
    return text
}

// todo: move extensions to own file base on dependencies, for example for these ones Either is
// needed
/**
 * Download a file in the public download folder
 *
 * @param source the file Uri
 * @param title the title to show on the notification
 * @param description the title to show on the notification
 * @param fileName the name to give to the downloaded file, title will be used if this is null
 * @return a Long identifying the download or null if an error has occurred
 */
fun DownloadManager.downloadFileInStandardFolder(
    source: Uri,
    title: String,
    description: String,
    fileName: String = title,
): EitherResult<Exception, Long> {
    return try {
        val request: DownloadManager.Request =
            DownloadManager.Request(source)
                .setTitle(title)
                .setDescription(description)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadID = this.enqueue(request)
        EitherResult.Success(downloadID)
    } catch (e: Exception) {
        Timber.e("Error starting download of ${source.path}, exception ${e.message}")
        EitherResult.Failure(e)
    }
}

/**
 * Return the Uri from a downloaded file id returned by the download manager
 *
 * @param id the file id
 * @return the file Uri or null if the id wasn't found or the download wasn't successful
 */
@SuppressLint("Range")
fun Context.getDownloadedFileUri(id: Long): Uri? {
    val manager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val cursor = manager.query(DownloadManager.Query().setFilterById(id))
    if (cursor.moveToFirst()) {
        val columnIndex: Int = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        if (cursor.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL)
            return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)).toUri()
    }
    cursor.close()
    return null
}

@SuppressLint("Range")
fun Uri.getFileName(context: Context): String {
    var fileName = ""
    when (this.scheme) {
        SCHEME_CONTENT -> {
            val cursor: Cursor? = context.contentResolver.query(this, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                cursor.close()
            }
        }
        else -> fileName = this.lastPathSegment ?: ""
    }
    return fileName
}

/**
 * Open an url from available apps
 *
 * @param url: the url to be opened
 * @param showErrorToast: set to true if an error toast should be displayed
 * @return true if the passed url is correct, false otherwise
 */
fun Context.openExternalWebPage(url: String, showErrorToast: Boolean = true): Boolean {
    // todo: check if app supporting this index are available, otherwise
    // android.content.ActivityNotFoundException can be thrown by this
    // this pattern accepts everything that is something.tld since there were too many new tlds and
    // Google gave up updating their regex
    if (url.isWebUrl()) {
        try {
            val webIntent =
                Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
            startActivity(webIntent)
        } catch (ex: android.content.ActivityNotFoundException) {
            Timber.e("Error opening externally a link ${ex.message}")
            showToast(R.string.browser_not_found, length = Toast.LENGTH_LONG)
        } catch (ex: SecurityException) {
            // the default app has marked itself as available to open these links
            // but does not have exported=true in its manifest activity
            Timber.e("Bugged app cannot receive external links ${ex.message}")
            showToast(R.string.invalid_player_found, length = Toast.LENGTH_LONG)
        }
        return true
    } else if (showErrorToast) showToast(R.string.invalid_url)

    return false
}

/**
 * Hand a media url to any installed player through a picker dialog, so the user picks the player
 * themselves instead of relying on a hardcoded list. Uses our own dialog rather than the system
 * chooser so VLC specifically can still be routed through [launchIntentForPlayer]'s safe shape
 * once picked; the system chooser dispatches the intent itself and gives no chance to adjust it
 * per choice.
 *
 * @param url the media url to open
 * @param mimeType the known mime type of the media, or null to guess it from the url
 * @return true if a picker was shown, false if no app could handle the intent
 */
fun Context.openMediaWithChooser(url: String, mimeType: String? = null): Boolean {
    val resolvedType =
        mimeType?.takeIf { it.isNotBlank() }
            ?: MimeTypeMap.getFileExtensionFromUrl(url)
                .takeIf { it.isNotEmpty() }
                ?.let {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase(Locale.ROOT))
                }
            ?: "video/*"
    val uri = url.toUri()
    val probeIntent =
        Intent(Intent.ACTION_VIEW).apply { setDataAndTypeAndNormalize(uri, resolvedType) }
    val apps =
        packageManager
            .queryIntentActivities(probeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) }
    if (apps.isEmpty()) {
        Timber.e("No app found to open media $url")
        showToast(R.string.app_not_installed, length = Toast.LENGTH_LONG)
        return false
    }
    // plain rows in a scrollable container rather than setAdapter's ListView: a ListView manages
    // its own single-selection focus, which fights with each row being its own d-pad focus stop
    val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val dialog =
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.open_with)
            .setView(ScrollView(this).apply { addView(container) })
            .create()
    apps.forEach { app ->
        val row = LayoutInflater.from(this).inflate(R.layout.item_open_with_app, container, false)
        row.findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(app.loadIcon(packageManager))
        row.findViewById<TextView>(R.id.tvAppLabel).text = app.loadLabel(packageManager)
        row.setOnClickListener {
            dialog.dismiss()
            val pkg = app.activityInfo.packageName
            try {
                startActivity(launchIntentForPlayer(pkg, uri, resolvedType))
            } catch (ex: ActivityNotFoundException) {
                Timber.e("Could not open media $url with $pkg: ${ex.message}")
                showToast(R.string.app_not_installed, length = Toast.LENGTH_LONG)
            }
        }
        container.addView(row)
    }
    dialog.show()
    return true
}

/** The bundled placeholder clip's file name once copied into the cache for FileProvider. */
private const val PLAYER_SETUP_CLIP_NAME = "player_setup_clip.mp4"

/**
 * Copy the bundled placeholder video clip to a cache file, reusing it as long as it still matches
 * the resource bundled in this build, and return a content:// [Uri] for it through this app's
 * FileProvider. External players cannot read a raw resource directly, so the settings screen hands
 * them this small real file to trigger Android's native "open with" chooser.
 */
fun Context.playerSetupClipUri(): Uri {
    val mediaDir = File(cacheDir, "media").apply { mkdirs() }
    val clip = File(mediaDir, PLAYER_SETUP_CLIP_NAME)
    val bundledSize = resources.openRawResourceFd(R.raw.player_setup_clip).use { it.length }
    if (!clip.exists() || clip.length() != bundledSize) {
        resources.openRawResource(R.raw.player_setup_clip).use { input ->
            clip.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", clip)
}

/**
 * Human readable label of the remembered preferred video player, or null when none is set yet, or
 * the remembered player is no longer installed (in which case the stale preference is cleared).
 */
fun Context.preferredVideoPlayerLabel(): CharSequence? {
    val pkg =
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(KEY_PREFERRED_VIDEO_PLAYER, null) ?: return null
    return try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0))
    } catch (e: PackageManager.NameNotFoundException) {
        clearPreferredVideoPlayer()
        null
    }
}

/** Forget the remembered preferred video player. Always works: it is only our own preference. */
fun Context.clearPreferredVideoPlayer() {
    PreferenceManager.getDefaultSharedPreferences(this)
        .edit()
        .remove(KEY_PREFERRED_VIDEO_PLAYER)
        .apply()
}

/**
 * Show the system's app chooser for [uri] so the user can pick a video player, remembering the
 * choice as the new preferred player via [VideoPlayerChosenReceiver]. Always shows the chooser
 * regardless of any previously remembered choice; call [playWithPreferredVideoPlayer] instead to go
 * straight to the remembered player when there is one.
 */
fun Context.pickVideoPlayer(uri: Uri) {
    val viewIntent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    val callbackFlags =
        if (SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    val callback =
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, VideoPlayerChosenReceiver::class.java),
            callbackFlags,
        )
    val chooser = Intent.createChooser(viewIntent, getString(R.string.open_with), callback.intentSender)
    try {
        startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Timber.e("No app found to open a video: ${e.message}")
        showToast(R.string.app_not_installed, length = Toast.LENGTH_LONG)
    }
}

// VLC's own StartActivity branches on the intent shape: a plain ACTION_VIEW races an eager,
// options-driven launch against this TV's window transitions and can silently fail to show video;
// ACTION_SEND with plain text (what Share already sends) takes VLC's lazier, service-mediated path
// instead and avoids the race. This only helps a remembered http(s) link, not the local placeholder
// clip used to pick a player in settings, since plain text sharing carries no read permission grant
// for a content:// uri.
private const val VLC_PACKAGE = "org.videolan.vlc"

/**
 * Build the intent that launches [pkg] for [uri], special-casing [VLC_PACKAGE] to use its safe
 * shape. Shared between [playWithPreferredVideoPlayer] and the [openMediaWithChooser] picker
 * dialog, since both need to apply the same VLC workaround once a package is chosen.
 */
private fun launchIntentForPlayer(pkg: String, uri: Uri, mimeType: String): Intent =
    if (pkg == VLC_PACKAGE) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri.toString())
            // set explicitly rather than relying on Android to synthesize it from EXTRA_TEXT:
            // this is what the share button already sends, and what VLC's own code branches on
            clipData = ClipData.newPlainText(null, uri.toString())
            setPackage(VLC_PACKAGE)
        }
    } else {
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            setPackage(pkg)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

/**
 * Play [uri] with the remembered preferred video player when one is set and still installed,
 * otherwise show the chooser so the user can pick one (and remember it for next time).
 */
fun Context.playWithPreferredVideoPlayer(uri: Uri) {
    val pkg =
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(KEY_PREFERRED_VIDEO_PLAYER, null)
    if (pkg == null) {
        pickVideoPlayer(uri)
        return
    }
    try {
        startActivity(launchIntentForPlayer(pkg, uri, "video/*"))
    } catch (e: ActivityNotFoundException) {
        // the remembered player can no longer handle this, forget it and let the user pick again
        clearPreferredVideoPlayer()
        pickVideoPlayer(uri)
    }
}

/**
 * this function can be used to create a new context with a particular locale. It must be used while
 * overriding Activity.attachBaseContext like this: override fun attachBaseContext(base: Context?) {
 * if (base != null) super.attachBaseContext(getUpdatedLocaleContext(base, "en")) else
 * super.attachBaseContext(null) } it must be applied to all the activities or added to a
 * BaseActivity extended by them
 */
fun Activity.getUpdatedLocaleContext(context: Context, language: String): Context {
    val locale = Locale.forLanguageTag(language)
    val configuration = Configuration(context.resources.configuration)
    // check if this is necessary
    Locale.setDefault(locale)
    configuration.setLocale(locale)
    return context.createConfigurationContext(configuration)
}

fun AppCompatActivity.setNavigationBarColor(color: Int, alpha: Int = 0) {
    val newColor = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    // set the color before applying the light bar effect
    window.navigationBarColor = newColor

    val luminance = Color.luminance(color)
    if (luminance >= 0.25) {
        when {
            SDK_INT >= Build.VERSION_CODES.R -> {
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                )
            }
            else -> {
                // the check above is not recognized
                @Suppress("DEPRECATION") @SuppressLint("InlinedApi")
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    } else
        @Suppress("DEPRECATION") @SuppressLint("InlinedApi")
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
        34 -> getString(R.string.too_many_requests)
        35 -> getString(R.string.infringing_file)
        36 -> getString(R.string.usage_limit_reached)
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
        "ready" -> getString(R.string.ready)
        "error" -> getString(R.string.error)
        "virus" -> getString(R.string.virus)
        "compressing" -> getString(R.string.compressing)
        "uploading" -> getString(R.string.uploading)
        "dead" -> getString(R.string.dead)
        else -> status
    }
}

@Suppress("DEPRECATION")
fun Context.vibrate(duration: Long = 200) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
}

/** AssetManager extensions */

/**
 * This function returns the list of files and folder found in a path of the assets folder, it
 * removes the "/" at the end and checks again if no files are found.
 */
fun AssetManager.smartList(path: String): Array<String>? {
    val result = this.list(path)
    if (result.isNullOrEmpty()) if (path.endsWith("/")) return this.list(path.dropLast(1))
    return result
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 34 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 34 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

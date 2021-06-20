package com.github.livingwithhippos.unchained.utilities.extension

import android.annotation.SuppressLint
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.Patterns
import com.github.livingwithhippos.unchained.utilities.CONTAINER_PATTERN
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.TORRENT_PATTERN
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * check if a String is an url
 */
fun String.isWebUrl(): Boolean =
    Patterns.WEB_URL.matcher(this).matches()

fun String.removeWebFormatting(): String {
    return this.replace("%3A", ":")
        .replace("%3F", "?")
        .replace("%3D", "=")
        .replace("%26", "&")
        .replace("%2B", "+")
        .replace("%25", "%")
}

/**
 * check if a String is a magnet link
 */
fun String?.isMagnet(): Boolean {
    if (this == null)
        return false
    val m: Matcher = Pattern.compile(MAGNET_PATTERN).matcher(this)
    return m.lookingAt()
}

/**
 * check if a String is a torrent link
 */
fun String?.isTorrent(): Boolean {
    if (this == null)
        return false
    val m: Matcher = Pattern.compile(TORRENT_PATTERN).matcher(this)
    return m.matches()
}

/**
 * check if a String is a container link
 */
fun String?.isContainerWebLink(): Boolean {
    if (this == null)
        return false
    val m: Matcher = Pattern.compile(CONTAINER_PATTERN).matcher(this)
    return m.lookingAt()
}

/**
 * parse a string from a custom date pattern to the current Locale pattern
 * @return the parsed String
 */
@SuppressLint("SimpleDateFormat")
fun String.toCustomDate(datePattern: String = "yyyy-MM-dd'T'hh:mm:ss"): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val originalDate: DateFormat = SimpleDateFormat(datePattern)
        val date: Date = originalDate.parse(this)
        val localDate: DateFormat = SimpleDateFormat.getDateTimeInstance()
        return localDate.format(date)
    } else {
        // todo: use a more robust option
        return this
    }
}

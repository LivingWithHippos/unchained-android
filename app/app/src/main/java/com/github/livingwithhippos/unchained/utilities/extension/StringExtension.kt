package com.github.livingwithhippos.unchained.utilities.extension

import android.annotation.SuppressLint
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.util.Patterns
import com.github.livingwithhippos.unchained.utilities.CONTAINER_PATTERN
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.TORRENT_PATTERN
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern

/** check if a String is an url */
fun String.isWebUrl(): Boolean = Patterns.WEB_URL.matcher(this).lookingAt()

const val urlPattern =
    "^https?://(-\\.)?([\\w]+\\.)+([\\w]{2,})+(#([\\w\\-]+))?(/[\\w\\-\\.,?^=%&:/~+#]*)?"

val simpleWebRegex = Regex(urlPattern)

fun String.isSimpleWebUrl(): Boolean = simpleWebRegex.containsMatchIn(this)

fun String.removeWebFormatting(): String {
    return this.replace("%3A", ":")
        .replace("%3F", "?")
        .replace("&#x3D;", "=")
        .replace("%3D", "=")
        .replace("%26", "&")
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("%2B", "+")
        .replace("%25", "%")
}

fun cleanScrapingResult(result: String): String {
    return result
        // remove styling tags
        .replace("<b\\s[^>]+>".toRegex(), "")
        .replace("</b>", "")
        // replace html tags
        .replace("%3A", ":")
        .replace("%3F", "?")
        .replace("&#x3D;", "=")
        .replace("%3D", "=")
        .replace("%26", "&")
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("%2B", "+")
        .replace("%25", "%")
        .replace("\\s+".toRegex(), " ")
        .replace("\n+".toRegex(), " ")
        .trim()
}

/**
 * format a search string to be used in a url query
 */
fun formatStringForSearch(query: String): String {
    return query.trim()
        .replace("\\s+".toRegex(), "+")
        // replace the % first since we use it as escape character after that
        .replace("%", "%25")
        .replace("&", "%26")
}

/** check if a String is a magnet link */
fun String?.isMagnet(): Boolean {
    if (this == null) return false
    val m: Matcher = Pattern.compile(MAGNET_PATTERN).matcher(this)
    return m.lookingAt()
}

/** check if a String is a torrent link */
fun String?.isTorrent(): Boolean {
    if (this == null) return false
    val m: Matcher = Pattern.compile(TORRENT_PATTERN).matcher(this)
    return m.matches()
}

/** check if a String is a container link */
fun String?.isContainerWebLink(): Boolean {
    if (this == null) return false
    val m: Matcher = Pattern.compile(CONTAINER_PATTERN).matcher(this)
    return m.lookingAt()
}

/**
 * parse a string from a custom date pattern to the current Locale pattern
 *
 * @return the parsed String
 */
@SuppressLint("SimpleDateFormat")
fun String.toCustomDate(datePattern: String = "yyyy-MM-dd'T'hh:mm:ss"): String {
    val originalDate: DateFormat = SimpleDateFormat(datePattern)
    val date: Date = originalDate.parse(this)
    val localDate: DateFormat = SimpleDateFormat.getDateTimeInstance()
    return localDate.format(date)
}

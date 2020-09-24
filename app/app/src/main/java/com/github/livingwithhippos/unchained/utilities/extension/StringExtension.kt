package com.github.livingwithhippos.unchained.utilities.extension

import android.annotation.SuppressLint
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.util.Patterns
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * check if a String is an url
 */
fun String.isWebUrl(): Boolean =
    Patterns.WEB_URL.matcher(this).matches()

/**
 * check if a String is a magnet link
 */
fun String.isMagnet(): Boolean {
    val m: Matcher = Pattern.compile(MAGNET_PATTERN).matcher(this)
    return m.lookingAt()
}

/**
 * parse a string from a custom date pattern to the current Locale pattern
 * @return the parsed String
 */
@SuppressLint("SimpleDateFormat")
fun String.toCustomDate(datePattern: String = "yyyy-MM-dd'T'hh:mm:ss"): String {
    val originalDate: DateFormat = SimpleDateFormat(datePattern)
    val date: Date = originalDate.parse(this)
    val localDate: DateFormat = SimpleDateFormat.getDateTimeInstance()
    return localDate.format(date)
}

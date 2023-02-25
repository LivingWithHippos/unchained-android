package com.github.livingwithhippos.unchained.utilities

val kbPattern = "\\s*(\\d\\d*\\.?\\d*)\\s*[kK]".toRegex()
val mbPattern = "\\s*(\\d\\d*\\.?\\d*)\\s*[mM]".toRegex()
val gbPattern = "\\s*(\\d\\d*\\.?\\d*)\\s*[gG]".toRegex()
val genericPatter = "\\d\\d*\\.?\\d*".toRegex()

fun parseCommonSize(rawSize: String?): Double? {
    try {
        if (rawSize.isNullOrBlank()) return null
        var match = kbPattern.find(rawSize)?.groupValues?.get(1)
        if (match != null) return match.toDouble() / 1024
        match = mbPattern.find(rawSize)?.groupValues?.get(1)
        if (match != null) return match.toDouble()
        match = gbPattern.find(rawSize)?.groupValues?.get(1)
        if (match != null) return match.toDouble() * 1024

        match = genericPatter.find(rawSize)?.value
        return match?.toDouble()
    } catch (e: NumberFormatException) {
        return null
    }
}

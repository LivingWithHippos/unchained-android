package com.github.livingwithhippos.unchained.plugins.model

import android.os.Parcelable
import com.github.livingwithhippos.unchained.plugins.Parser
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

// todo: replace all the Parcelable with the kotlin library
@JsonClass(generateAdapter = true)
@Parcelize
data class Plugin(
    @Json(name = "engine_version") val engineVersion: Float,
    @Json(name = "version") val version: Float,
    @Json(name = "url") val url: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?,
    @Json(name = "author") val author: String?,
    @Json(name = "supported_categories") val supportedCategories: SupportedCategories,
    @Json(name = "search") val search: PluginSearch,
    @Json(name = "download") val download: PluginDownload
) : Parcelable {
    fun isCompatible(): Boolean {
        return engineVersion.toInt() == Parser.PLUGIN_ENGINE_VERSION.toInt() &&
            Parser.PLUGIN_ENGINE_VERSION >= engineVersion
    }
}

fun isCompatible(engineVersion: Double): Boolean {
    return engineVersion.toInt() == Parser.PLUGIN_ENGINE_VERSION.toInt() &&
        Parser.PLUGIN_ENGINE_VERSION >= engineVersion
}

@JsonClass(generateAdapter = true)
@Parcelize
data class SupportedCategories(
    @Json(name = "all") val all: String,
    @Json(name = "art") val art: String?,
    @Json(name = "anime") val anime: String?,
    @Json(name = "doujinshi") val doujinshi: String?,
    @Json(name = "manga") val manga: String?,
    @Json(name = "software") val software: String?,
    @Json(name = "games") val games: String?,
    @Json(name = "movies") val movies: String?,
    @Json(name = "pictures") val pictures: String?,
    @Json(name = "videos") val videos: String?,
    @Json(name = "music") val music: String?,
    @Json(name = "tv") val tv: String?,
    @Json(name = "books") val books: String?
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginSearch(
    @Json(name = "category") val urlCategory: String?,
    @Json(name = "no_category") val urlNoCategory: String,
    @Json(name = "page_start") val pageStart: Int? = 1
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginDownload(
    @Json(name = "internal") val internalParser: InternalParser?,
    @Json(name = "direct") val directParser: DirectParser?,
    @Json(name = "table_direct") val tableLink: TableParser?,
    @Json(name = "table_indirect") val indirectTableLink: TableParser?,
    @Json(name = "regexes") val regexes: PluginRegexes
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class RegexpsGroup(
    @Json(name = "regex_use") val regexUse: String = "first",
    @Json(name = "regexps") val regexps: List<CustomRegex>
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class CustomRegex(
    @Json(name = "regex") val regex: String,
    @Json(name = "group") val group: Int = 1,
    @Json(name = "slug_type") val slugType: String = "complete",
    @Json(name = "other") val other: String?
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class InternalParser(@Json(name = "link") val link: RegexpsGroup) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class Internal(@Json(name = "link") val link: RegexpsGroup) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class TableParser(
    @Json(name = "class") val className: String?,
    @Json(name = "id") val idName: String?,
    @Json(name = "index") val index: Int?,
    @Json(name = "columns") val columns: Columns
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class DirectParser(
    @Json(name = "class") val className: String?,
    @Json(name = "id") val idName: String?,
    @Json(name = "entry-class") val entryClass: String
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginRegexes(
    @Json(name = "name") val nameRegex: RegexpsGroup,
    @Json(name = "seeders") val seedersRegex: RegexpsGroup?,
    @Json(name = "leechers") val leechersRegex: RegexpsGroup?,
    @Json(name = "size") val sizeRegex: RegexpsGroup?,
    @Json(name = "magnet") val magnetRegex: RegexpsGroup?,
    @Json(name = "torrents") val torrentRegexes: RegexpsGroup?,
    @Json(name = "hosting") val hostingRegexes: RegexpsGroup?,
    @Json(name = "details") val detailsRegex: RegexpsGroup?
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class Columns(
    @Json(name = "name_column") val nameColumn: Int?,
    @Json(name = "seeders_column") val seedersColumn: Int?,
    @Json(name = "leechers_column") val leechersColumn: Int?,
    @Json(name = "size_column") val sizeColumn: Int?,
    @Json(name = "magnet_column") val magnetColumn: Int?,
    @Json(name = "torrent_column") val torrentColumn: Int?,
    @Json(name = "details_column") val detailsColumn: Int?,
    @Json(name = "hosting_column") val hostingColumn: Int?
) : Parcelable

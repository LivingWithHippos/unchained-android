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
    @param:Json(name = "engine_version") val engineVersion: Float,
    @param:Json(name = "version") val version: Float,
    @param:Json(name = "url") val url: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "author") val author: String?,
    @param:Json(name = "supported_categories") val supportedCategories: SupportedCategories,
    @param:Json(name = "search") val search: PluginSearch,
    @param:Json(name = "download") val download: PluginDownload,
    /**
     * Selected plugins are the ones enabled for the mass search in the Plugin Search Fragment.
     */
    @param:Json(name = "selected") var selected: Boolean?,
    /**
     * Repository is the path or url of the repository, something to distinguish plugins with the same name
     */
    @param:Json(name = "repository") var repository: String?
) : Parcelable {
    fun isCompatible(): Boolean {
        return engineVersion.toInt() == Parser.PLUGIN_ENGINE_VERSION.toInt() &&
            Parser.PLUGIN_ENGINE_VERSION >= engineVersion
    }
}

fun isCompatible(engineVersion: Float): Boolean {
    return engineVersion.toInt() == Parser.PLUGIN_ENGINE_VERSION.toInt() &&
        Parser.PLUGIN_ENGINE_VERSION >= engineVersion
}

@JsonClass(generateAdapter = true)
@Parcelize
data class SupportedCategories(
    @param:Json(name = "all") val all: String,
    @param:Json(name = "art") val art: String?,
    @param:Json(name = "anime") val anime: String?,
    @param:Json(name = "doujinshi") val doujinshi: String?,
    @param:Json(name = "manga") val manga: String?,
    @param:Json(name = "software") val software: String?,
    @param:Json(name = "games") val games: String?,
    @param:Json(name = "movies") val movies: String?,
    @param:Json(name = "pictures") val pictures: String?,
    @param:Json(name = "videos") val videos: String?,
    @param:Json(name = "music") val music: String?,
    @param:Json(name = "tv") val tv: String?,
    @param:Json(name = "books") val books: String?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginSearch(
    @param:Json(name = "category") val urlCategory: String?,
    @param:Json(name = "no_category") val urlNoCategory: String,
    @param:Json(name = "page_start") val pageStart: Int? = 1,
    @param:Json(name = "sorting") val sorting: PluginSorting?,
    @param:Json(name = "order") val order: PluginOrdering?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginSorting(
    @param:Json(name = "comments") val comments: String?,
    @param:Json(name = "downloads") val downloads: String?,
    @param:Json(name = "size") val size: String?,
    @param:Json(name = "date") val date: String?,
    @param:Json(name = "seeders") val seeders: String?,
    @param:Json(name = "leechers") val leechers: String?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginOrdering(
    @param:Json(name = "ascending") val ascending: String?,
    @param:Json(name = "descending") val descending: String?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginDownload(
    @param:Json(name = "internal") val internalParser: InternalParser?,
    @param:Json(name = "direct") val directParser: DirectParser?,
    @param:Json(name = "table_direct") val tableLink: TableParser?,
    @param:Json(name = "table_indirect") val indirectTableLink: TableParser?,
    @param:Json(name = "regexes") val regexes: PluginRegexes,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class RegexpsGroup(
    @param:Json(name = "regex_use") val regexUse: String = "first",
    @param:Json(name = "regexps") val regexps: List<CustomRegex>,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class CustomRegex(
    @param:Json(name = "regex") val regex: String,
    @param:Json(name = "group") val group: Int = 1,
    @param:Json(name = "slug_type") val slugType: String = "complete",
    @param:Json(name = "other") val other: String?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class InternalParser(@param:Json(name = "link") val link: RegexpsGroup) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class Internal(@param:Json(name = "link") val link: RegexpsGroup) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class TableParser(
    @param:Json(name = "class") val className: String?,
    @param:Json(name = "id") val idName: String?,
    @param:Json(name = "index") val index: Int?,
    @param:Json(name = "columns") val columns: Columns,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class DirectParser(
    @param:Json(name = "class") val className: String?,
    @param:Json(name = "id") val idName: String?,
    @param:Json(name = "entry-class") val entryClass: String?,
    @param:Json(name = "entry-tag") val entryTag: String?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PluginRegexes(
    @param:Json(name = "name") val nameRegex: RegexpsGroup,
    @param:Json(name = "seeders") val seedersRegex: RegexpsGroup?,
    @param:Json(name = "leechers") val leechersRegex: RegexpsGroup?,
    @param:Json(name = "size") val sizeRegex: RegexpsGroup?,
    @param:Json(name = "date_added") val dateAddedRegex: RegexpsGroup?,
    @param:Json(name = "magnet") val magnetRegex: RegexpsGroup?,
    @param:Json(name = "torrents") val torrentRegexes: RegexpsGroup?,
    @param:Json(name = "hosting") val hostingRegexes: RegexpsGroup?,
    @param:Json(name = "details") val detailsRegex: RegexpsGroup?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class Columns(
    @param:Json(name = "name_column") val nameColumn: Int?,
    @param:Json(name = "seeders_column") val seedersColumn: Int?,
    @param:Json(name = "leechers_column") val leechersColumn: Int?,
    @param:Json(name = "added_date_column") val addedDateColumn: Int?,
    @param:Json(name = "size_column") val sizeColumn: Int?,
    @param:Json(name = "magnet_column") val magnetColumn: Int?,
    @param:Json(name = "torrent_column") val torrentColumn: Int?,
    @param:Json(name = "details_column") val detailsColumn: Int?,
    @param:Json(name = "hosting_column") val hostingColumn: Int?,
) : Parcelable

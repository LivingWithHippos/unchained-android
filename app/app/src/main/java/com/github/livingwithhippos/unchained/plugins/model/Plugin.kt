package com.github.livingwithhippos.unchained.plugins.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class Plugin(
    @Json(name = "engine_version")
    val engineVersion: Double,
    @Json(name = "version")
    val version: Double,
    @Json(name = "url")
    val url: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "description")
    val description: String?,
    @Json(name = "supported_categories")
    val supportedCategories: SupportedCategories,
    @Json(name = "search")
    val search: PluginSearch,
    @Json(name = "download")
    val download: PluginDownload
)

@JsonClass(generateAdapter = true)
data class SupportedCategories(
    @Json(name = "all")
    val all: String?,
    @Json(name = "anime")
    val anime: String?,
    @Json(name = "software")
    val software: String?,
    @Json(name = "games")
    val games: String?,
    @Json(name = "movies")
    val movies: String?,
    @Json(name = "music")
    val music: String?,
    @Json(name = "tv")
    val tv: String?
)

@JsonClass(generateAdapter = true)
data class PluginSearch(
    @Json(name = "category")
    val urlCategory: String?,
    @Json(name = "no_category")
    val urlNoCategory: String
)

@JsonClass(generateAdapter = true)
data class PluginDownload(
    @Json(name = "name")
    val name: String,
    @Json(name = "magnet")
    val magnet: String,
    @Json(name = "torrent")
    val torrent: List<String>,
    @Json(name = "internal")
    val internalLink: InternalLink?
)

@JsonClass(generateAdapter = true)
data class InternalLink(
    @Json(name = "link")
    val link: String,
    @Json(name = "slug_type")
    val slugType: String,
    @Json(name = "other")
    val other: String?
)
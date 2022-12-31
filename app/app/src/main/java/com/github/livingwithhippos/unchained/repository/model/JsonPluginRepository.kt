package com.github.livingwithhippos.unchained.repository.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class JsonPluginRepository(
    @Json(name = "repository_version")
    val repositoryVersion: Double,
    @Json(name = "name")
    val name: String,
    @Json(name = "description")
    val description: String,
    @Json(name = "author")
    val author: String,
    @Json(name = "plugins")
    val plugins: List<JsonPlugin>
)

@JsonClass(generateAdapter = true)
data class JsonPlugin(
    @Json(name = "id")
    val id: String,
    @Json(name = "versions")
    val versions: List<JsonPluginVersion>
)

@JsonClass(generateAdapter = true)
data class JsonPluginVersion(
    @Json(name = "plugin")
    val plugin: Float,
    @Json(name = "engine")
    val engine: Double,
    @Json(name = "link")
    val link: String
)
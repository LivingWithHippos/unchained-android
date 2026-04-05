package com.github.livingwithhippos.unchained.repository.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonPluginRepository(
    @param:Json(name = "repository_version") val repositoryVersion: Double,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "description") val description: String,
    @param:Json(name = "author") val author: String,
    @param:Json(name = "plugins") val plugins: List<JsonPlugin>,
)

@JsonClass(generateAdapter = true)
data class JsonPlugin(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "disabled") val disabled: Boolean?,
    @param:Json(name = "versions") val versions: List<JsonPluginVersion>,
)

@JsonClass(generateAdapter = true)
data class JsonPluginVersion(
    @param:Json(name = "plugin") val plugin: Float,
    @param:Json(name = "engine") val engine: Float,
    @param:Json(name = "link") val link: String,
)

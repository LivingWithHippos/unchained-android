package com.github.livingwithhippos.unchained.search.model
import com.squareup.moshi.JsonClass

import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class RemotePlugin(
    @Json(name = "id")
    val id: String,
    @Json(name = "versions")
    val versions: List<PluginVersion>,
    @Json(name = "status")
    var status: String?
)

@JsonClass(generateAdapter = true)
data class PluginVersion(
    @Json(name = "engine")
    val engine: Double,
    @Json(name = "link")
    val link: String,
    @Json(name = "plugin")
    val plugin: Double
)

object PluginStatus {
    const val isNew = "NEW"
    const val hasUpdate = "HAS_UPDATE"
    const val ready = "READY"
    const val uncompatible = "UNCOMPATIBLE"
}
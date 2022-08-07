package com.github.livingwithhippos.unchained.data.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Updates(
    @Json(name = "play_store")
    val playStore: VersionData?,
    @Json(name = "f_droid")
    val fDroid: VersionData?,
    @Json(name = "github")
    val github: VersionData?
)

@JsonClass(generateAdapter = true)
data class VersionData(
    @Json(name = "signature")
    val signature: String,
    @Json(name = "versionCode")
    val versionCode: Int
)

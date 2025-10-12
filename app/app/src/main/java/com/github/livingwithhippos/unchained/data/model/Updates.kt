package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Updates(
    @param:Json(name = "play_store") val playStore: VersionData?,
    @param:Json(name = "f_droid") val fDroid: VersionData?,
    @param:Json(name = "github") val github: VersionData?,
)

@JsonClass(generateAdapter = true)
data class VersionData(
    @param:Json(name = "signature") val signature: String,
    @param:Json(name = "versionCode") val versionCode: Int,
)

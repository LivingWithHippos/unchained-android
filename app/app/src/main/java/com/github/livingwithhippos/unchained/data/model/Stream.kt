package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Stream(
    @param:Json(name = "h264WebM") val h264WebM: Quality,
    @param:Json(name = "liveMP4") val liveMP4: Quality,
    @param:Json(name = "apple") val apple: Quality,
    @param:Json(name = "dash") val dash: Quality,
)

@JsonClass(generateAdapter = true) data class Quality(@param:Json(name = "full") val link: String)

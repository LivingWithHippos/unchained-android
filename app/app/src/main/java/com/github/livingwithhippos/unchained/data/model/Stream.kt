package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Stream(
    @Json(name = "h264WebM") val h264WebM: Quality,
    @Json(name = "liveMP4") val liveMP4: Quality,
    @Json(name = "apple") val apple: Quality,
    @Json(name = "dash") val dash: Quality
)

@JsonClass(generateAdapter = true) data class Quality(@Json(name = "full") val link: String)

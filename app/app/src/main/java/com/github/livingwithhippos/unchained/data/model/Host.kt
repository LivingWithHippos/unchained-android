package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// fixme: the key is variable, the same as HostStatus.name, find out how to model that @Json(name =
// ?)
@JsonClass(generateAdapter = true)
data class Host(@Json(name = "id") val hostStatus: Map<String, HostStatus>)

@JsonClass(generateAdapter = true)
data class HostStatus(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "image") val image: String,
    @Json(name = "image_big") val imageBig: String,
    @Json(name = "supported") val supported: String,
    @Json(name = "status") val status: String,
    @Json(name = "check_time") val checkTime: String,
    @Json(name = "competitors_status") val competitorsStatus: Map<String, Competitor>,
)

@JsonClass(generateAdapter = true)
data class Competitor(
    @Json(name = "status") val status: String,
    @Json(name = "check_time") val checkTime: String,
)

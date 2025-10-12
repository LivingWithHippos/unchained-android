package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// fixme: the key is variable, the same as HostStatus.name, find out how to model that
// @param:Json(name =
// ?)
@JsonClass(generateAdapter = true)
data class Host(@param:Json(name = "id") val hostStatus: Map<String, HostStatus>)

@JsonClass(generateAdapter = true)
data class HostStatus(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "image") val image: String,
    @param:Json(name = "image_big") val imageBig: String,
    @param:Json(name = "supported") val supported: String,
    @param:Json(name = "status") val status: String,
    @param:Json(name = "check_time") val checkTime: String,
    @param:Json(name = "competitors_status") val competitorsStatus: Map<String, Competitor>,
)

@JsonClass(generateAdapter = true)
data class Competitor(
    @param:Json(name = "status") val status: String,
    @param:Json(name = "check_time") val checkTime: String,
)

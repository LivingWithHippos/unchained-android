package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * KODI returned values
 *
 * If everything is ok: { "id": 1, "jsonrpc": "2.0", "result": "OK" }
 *
 * Some error occurred: { "error": { "code": -32600, "message": "Invalid request." }, "id": 1,
 * "jsonrpc": "2.0" }
 */
@JsonClass(generateAdapter = true)
data class KodiRequest(
    @param:Json(name = "jsonrpc") val jsonRPC: String = "2.0",
    @param:Json(name = "id") val id: Int = 616,
    @param:Json(name = "method") val method: String,
    @param:Json(name = "params") val params: KodiParams,
)

@JsonClass(generateAdapter = true)
data class KodiParams(
    @param:Json(name = "item") val item: KodiItem? = null,
    @param:Json(name = "properties") val properties: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class KodiItem(@param:Json(name = "file") val fileUrl: String)

@JsonClass(generateAdapter = true)
data class KodiResponse(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "jsonrpc") val jsonrpc: String,
    @param:Json(name = "result") val result: String,
)

@JsonClass(generateAdapter = true)
data class KodiGenericResponse(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "jsonrpc") val jsonrpc: String,
    @param:Json(name = "result") val result: Any,
)

@JsonClass(generateAdapter = true)
data class KodiError(
    @param:Json(name = "error") val error: KodiErrorData,
    @param:Json(name = "id") val type: String?,
)

@JsonClass(generateAdapter = true)
data class KodiErrorData(
    @param:Json(name = "code") val code: Int,
    @param:Json(name = "message") val message: String,
)

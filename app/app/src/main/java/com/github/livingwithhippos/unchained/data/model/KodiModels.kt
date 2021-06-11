package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * KODI returned values
 *
 * If everything is ok:
{
"id": 1,
"jsonrpc": "2.0",
"result": "OK"
}

 * Some error occurred:
{
"error": {
"code": -32600,
"message": "Invalid request."
},
"id": 1,
"jsonrpc": "2.0"
}
 */

@JsonClass(generateAdapter = true)
data class KodiRequest(
    @Json(name = "jsonrpc")
    val jsonRPC: String = "2.0",
    @Json(name = "id")
    val id: Int = 616,
    @Json(name = "method")
    val method: String,
    @Json(name = "params")
    val params: KodiParams
)

@JsonClass(generateAdapter = true)
data class KodiParams(
    @Json(name = "item")
    val item: KodiItem? = null,
    @Json(name = "properties")
    val properties: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class KodiItem(
    @Json(name = "file")
    val fileUrl: String
)

@JsonClass(generateAdapter = true)
data class KodiResponse(
    @Json(name = "id")
    val id: Int,
    @Json(name = "jsonrpc")
    val jsonrpc: String,
    @Json(name = "result")
    val result: String
)

@JsonClass(generateAdapter = true)
data class KodiGenericResponse(
    @Json(name = "id")
    val id: Int,
    @Json(name = "jsonrpc")
    val jsonrpc: String,
    @Json(name = "result")
    val result: Any
)

@JsonClass(generateAdapter = true)
data class KodiError(
    @Json(name = "error")
    val error: KodiErrorData,
    @Json(name = "id")
    val type: String?
)

@JsonClass(generateAdapter = true)
data class KodiErrorData(
    @Json(name = "code")
    val code: Int,
    @Json(name = "message")
    val message: String
)

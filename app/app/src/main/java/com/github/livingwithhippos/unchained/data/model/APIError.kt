package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class APIError(
    @Json(name = "error")
    val error: String,
    @Json(name = "error_details")
    val errorDetails: String?,
    @Json(name = "error_code")
    val errorCode: Int?
)

/**
 * Manager the response error body from the retrofit calls. WIP.
 */
class APIException(val apiError: APIError) : Exception()
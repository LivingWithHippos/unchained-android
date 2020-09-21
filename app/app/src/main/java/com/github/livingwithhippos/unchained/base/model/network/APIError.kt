package com.github.livingwithhippos.unchained.base.model.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class APIError(
    @Json(name = "error")
    val error: String,
    @Json(name = "error_code")
    val errorCode: Int?
)

class APIException(val apiError: APIError): Exception() {
}
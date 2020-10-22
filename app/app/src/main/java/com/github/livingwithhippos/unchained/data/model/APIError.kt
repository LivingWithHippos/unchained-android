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
) : UnchainedNetworkException

// todo: this has been resolved by adding an interceptor, change class name at least
data class EmptyBodyError(
    val returnCode: Int
) : UnchainedNetworkException

data class NetworkError(
    val error: Int,
    val message: String
) : UnchainedNetworkException

data class ApiConversionError(
    val error: Int
) : UnchainedNetworkException

interface UnchainedNetworkException
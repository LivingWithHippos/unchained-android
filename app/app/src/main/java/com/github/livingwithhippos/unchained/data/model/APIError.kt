package com.github.livingwithhippos.unchained.data.model

import com.github.livingwithhippos.unchained.utilities.errorMap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import timber.log.Timber

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

/**
 * Helper function to quickly debug network errors. Will output them using Timber.
 *
 */
fun UnchainedNetworkException.printError() {
    when (this) {
        is APIError -> Timber.d(errorMap[this.errorCode ?: -1])
        is EmptyBodyError -> Timber.d("Empty Body error, return code: ${this.returnCode}")
        is NetworkError -> Timber.d("Network error, message: ${this.message}")
        is ApiConversionError -> Timber.d("Api Conversion error, error: ${this.error}")
    }
}

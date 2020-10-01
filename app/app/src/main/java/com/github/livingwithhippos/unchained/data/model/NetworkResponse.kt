package com.github.livingwithhippos.unchained.data.model

/*
 * see https://kotlinlang.org/docs/reference/generics.html#declaration-site-variance for an explanation of the out keyword.
 * T and U will be used as return types
 */

//todo: add loading class
/**
 * Sealed classes representing all the possible network responses
 */
sealed class NetworkResponse<out T : Any> {
    data class Success<out T : Any>(val data: T) : NetworkResponse<T>()
    data class SuccessEmptyBody(val code: Int) : NetworkResponse<Nothing>()
    data class Error(val exception: Exception) : NetworkResponse<Nothing>()
}

sealed class CompleteNetworkResponse<out T : Any?, out U: APIError?> {
    data class Success<out T : Any>(val data: T) : CompleteNetworkResponse<T, Nothing>()
    data class SuccessEmptyBody(val code: Int) : CompleteNetworkResponse<Nothing, Nothing>()
    data class Error(val errorMessage: String) : CompleteNetworkResponse<Nothing, Nothing>()
    data class RDError<out U: APIError?>(val error: U) : CompleteNetworkResponse<Nothing, APIError?>()
}
package com.github.livingwithhippos.unchained.data.model

/*
 * see https://kotlinlang.org/docs/reference/generics.html#declaration-site-variance for an explanation of the out keyword.
 * T and U will be used as return types
 */

// todo: add loading class
/** Sealed classes representing all the possible network responses */
sealed class NetworkResponse<out T : Any> {
    data class Success<out T : Any>(val data: T) : NetworkResponse<T>()

    data class SuccessEmptyBody(val code: Int) : NetworkResponse<Nothing>()

    data class Error(val exception: Exception) : NetworkResponse<Nothing>()
}

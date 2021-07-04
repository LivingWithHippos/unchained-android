package com.github.livingwithhippos.unchained.utilities

sealed class EitherResult<out T : Any, out U : Any> {
    data class Failure<out T : Any>(val failure: T) : EitherResult<T, Nothing>()
    data class Success<out U : Any>(val success: U) : EitherResult<Nothing, U>()
}

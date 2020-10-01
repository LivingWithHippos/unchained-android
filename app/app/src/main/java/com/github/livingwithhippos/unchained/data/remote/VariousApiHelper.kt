package com.github.livingwithhippos.unchained.data.remote

import retrofit2.Response

interface VariousApiHelper {
    suspend fun disableToken(token: String): Response<Unit>
}
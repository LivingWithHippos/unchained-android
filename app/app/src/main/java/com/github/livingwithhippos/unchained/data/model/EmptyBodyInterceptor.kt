package com.github.livingwithhippos.unchained.data.model

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

object EmptyBodyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful || response.code.let { it != 204 && it != 205 }) {
            return response
        }

        if ((response.body?.contentLength() ?: -1) >= 0) {
            return response.newBuilder().code(200).build()
        }

        // optionally we couyld return a new JSON item with the original code in it
        // val emptyBody = "{original_code: ${response.code} }".toResponseBody("application/json".toMediaType())

        val emptyBody = "".toResponseBody("text/plain".toMediaType())

        return response
            .newBuilder()
            .code(200)
            .body(emptyBody)
            .build()
    }
}
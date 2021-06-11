package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.KodiGenericResponse
import com.github.livingwithhippos.unchained.data.model.KodiRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface KodiApi {

    @Headers("Content-Type: application/json")
    @POST("jsonrpc")
    suspend fun openUrl(
        @Body body: KodiRequest
    ): Response<KodiResponse>

    @Headers("Content-Type: application/json")
    @POST("jsonrpc")
    suspend fun getVolume(
        @Body body: KodiRequest
    ): Response<KodiGenericResponse>

}
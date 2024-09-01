package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.KodiGenericResponse
import com.github.livingwithhippos.unchained.data.model.KodiRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface KodiApi {

    @POST("jsonrpc")
    suspend fun openUrl(
        @Body body: KodiRequest,
        @Header("Authorization") auth: String? = null,
        @Header("Content-Type") contentType: String = "application/json",
    ): Response<KodiResponse>

    @POST("jsonrpc")
    suspend fun getVolume(
        @Body body: KodiRequest,
        @Header("Authorization") auth: String? = null,
        @Header("Content-Type") contentType: String = "application/json",
    ): Response<KodiGenericResponse>
}

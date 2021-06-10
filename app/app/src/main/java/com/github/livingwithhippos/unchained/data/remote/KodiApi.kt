package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.KodiOpenRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface KodiApi {

    @Headers("Content-Type: application/json")
    @POST("jsonrpc")
    suspend fun openUrl(
        @Body body: KodiOpenRequest
    ): Response<KodiResponse>

}
package com.github.livingwithhippos.unchained.data.remote

import androidx.annotation.Keep
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

@Keep
/** This interface is used by Retrofit to manage various api calls not fitting elsewhere */
interface VariousApi {
    /** Disable the current access token */
    @GET("disable_access_token")
    suspend fun disableToken(@Header("Authorization") token: String): Response<Unit>
}

package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Stream
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * This interface is used by Retrofit to manage all the REST calls to the endpoints needed to
 * retrieve streaming links from a download The APIs in this interface will not work with an open
 * source token.
 */
interface StreamingApi {

    @GET("streaming/transcode/{id}")
    suspend fun getStreams(
        @Header("Authorization") token: String,
        @Path("id") id: String,
    ): Response<Stream>
}

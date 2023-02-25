package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Updates
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface UpdateApi {
    @GET suspend fun getUpdates(@Url url: String): Response<Updates>
}

interface UpdateApiHelper {

    suspend fun getUpdates(url: String): Response<Updates>
}

package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Stream
import retrofit2.Response

interface StreamingApiHelper {
    suspend fun getStreams(token: String, id: String): Response<Stream>
}

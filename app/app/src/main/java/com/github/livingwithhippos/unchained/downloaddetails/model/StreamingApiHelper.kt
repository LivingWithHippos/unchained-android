package com.github.livingwithhippos.unchained.downloaddetails.model

import com.github.livingwithhippos.unchained.user.model.User
import retrofit2.Response

interface StreamingApiHelper {
    suspend fun getStreams(token: String, id: String): Response<Stream>
}
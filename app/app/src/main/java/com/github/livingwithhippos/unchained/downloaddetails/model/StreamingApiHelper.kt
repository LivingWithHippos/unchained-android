package com.github.livingwithhippos.unchained.downloaddetails.model

import retrofit2.Response

interface StreamingApiHelper {
    suspend fun getStreams(token: String, id: String): Response<Stream>
}
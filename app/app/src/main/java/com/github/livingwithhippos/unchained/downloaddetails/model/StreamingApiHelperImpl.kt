package com.github.livingwithhippos.unchained.downloaddetails.model

import retrofit2.Response
import javax.inject.Inject

class StreamingApiHelperImpl @Inject constructor(private val streamingApi: StreamingApi) :
    StreamingApiHelper {
    override suspend fun getStreams(token: String, id: String): Response<Stream> =
        streamingApi.getStreams(token, id)
}
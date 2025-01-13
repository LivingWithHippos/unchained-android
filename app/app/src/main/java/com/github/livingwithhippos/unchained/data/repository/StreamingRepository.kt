package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.remote.StreamingApiHelper

class StreamingRepository(protoStore: ProtoStore, private val streamingApiHelper: StreamingApiHelper) :
    BaseRepository(protoStore) {

    suspend fun getStreams(id: String): Stream? {

        val streamResponse =
            safeApiCall(
                call = { streamingApiHelper.getStreams("Bearer ${getToken()}", id) },
                errorMessage = "Error Fetching Streaming Info",
            )

        return streamResponse
    }
}

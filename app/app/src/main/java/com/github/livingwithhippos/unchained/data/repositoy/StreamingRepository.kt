package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.remote.StreamingApiHelper
import javax.inject.Inject

class StreamingRepository @Inject constructor(private val streamingApiHelper: StreamingApiHelper) :
    BaseRepository() {

    suspend fun getStreams(token: String, id: String): Stream? {

        val streamResponse = safeApiCall(
            call = { streamingApiHelper.getStreams("Bearer $token", id) },
            errorMessage = "Error Fetching Streaming Info"
        )

        return streamResponse
    }
}

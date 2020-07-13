package com.github.livingwithhippos.unchained.downloaddetails.model

import com.github.livingwithhippos.unchained.base.model.repositories.BaseRepository
import javax.inject.Inject

class StreamingRepository @Inject constructor(private val streamingApiHelper: StreamingApiHelper) : BaseRepository() {

    suspend fun getStreams(token: String, id: String): Stream? {

        val streamResponse = safeApiCall(
            call = { streamingApiHelper.getStreams("Bearer $token", id) },
            errorMessage = "Error Fetching Streaming Info"
        )

        return streamResponse;

    }
}
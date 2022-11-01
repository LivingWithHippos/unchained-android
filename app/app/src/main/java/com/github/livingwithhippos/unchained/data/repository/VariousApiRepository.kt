package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.remote.VariousApiHelper
import javax.inject.Inject

class VariousApiRepository @Inject constructor(
    private val protoStore: ProtoStore,
    private val variousApiHelper: VariousApiHelper
) :
    BaseRepository(protoStore) {

    suspend fun disableToken(): Unit? {

        val response = safeApiCall(
            call = {
                variousApiHelper.disableToken(
                    token = "Bearer ${getToken()}"
                )
            },
            errorMessage = "Error disabling token"
        )

        return response
    }
}

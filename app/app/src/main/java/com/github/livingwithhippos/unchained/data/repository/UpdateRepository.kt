package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.Updates
import com.github.livingwithhippos.unchained.data.remote.UpdateApiHelper
import com.github.livingwithhippos.unchained.utilities.SIGNATURE
import javax.inject.Inject

class UpdateRepository @Inject constructor(
    private val protoStore: ProtoStore,
    private val updateApiHelper: UpdateApiHelper
) :
    BaseRepository(protoStore) {

    suspend fun getUpdates(url: String = SIGNATURE.URL): Updates? {

        val response = safeApiCall(
            call = {
                updateApiHelper.getUpdates(url)
            },
            errorMessage = "Error getting updates"
        )

        return response
    }
}

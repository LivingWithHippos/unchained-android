package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.model.Updates
import com.github.livingwithhippos.unchained.data.remote.UpdateApiHelper
import com.github.livingwithhippos.unchained.utilities.SIGNATURE
import javax.inject.Inject

class UpdateRepository  @Inject constructor(private val updateApiHelper: UpdateApiHelper) :
    BaseRepository() {

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

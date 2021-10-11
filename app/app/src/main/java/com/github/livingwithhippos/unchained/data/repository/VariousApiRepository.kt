package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.remote.VariousApiHelper
import javax.inject.Inject

class VariousApiRepository @Inject constructor(private val variousApiHelper: VariousApiHelper) :
    BaseRepository() {

    suspend fun disableToken(token: String): Unit? {

        val response = safeApiCall(
            call = {
                variousApiHelper.disableToken(
                    token = "Bearer $token"
                )
            },
            errorMessage = "Error disabling token"
        )

        return response
    }
}

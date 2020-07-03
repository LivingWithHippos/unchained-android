package com.github.livingwithhippos.unchained.authentication.model

import com.github.livingwithhippos.unchained.base.model.BaseRepository

class AuthenticationRepository(private val api: AuthenticationApi) : BaseRepository() {

    suspend fun getVerificationCode(): Authentication? {

        val authResponse = safeApiCall(
            call = { api.getAuthentication() },
            errorMessage = "Error Fetching Authentication Info"
        )

        return authResponse;

    }
}
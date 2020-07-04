package com.github.livingwithhippos.unchained.authentication.model

import com.github.livingwithhippos.unchained.base.model.repositories.BaseRepository

class AuthenticationRepository(private val api: AuthenticationApi) : BaseRepository() {

    suspend fun getVerificationCode(): Authentication? {

        val authResponse = safeApiCall(
            call = { api.getAuthentication() },
            errorMessage = "Error Fetching Authentication Info"
        )

        return authResponse;

    }

    suspend fun getSecrets(code: String): Secrets? {

        val authResponse = safeApiCall(
            call = { api.getSecrets(deviceCode = code) },
            errorMessage = "Error Fetching Secrets"
        )

        return authResponse;

    }

    suspend fun getToken(clientSecret: String, deviceCode: String): Token? {

        val authResponse = safeApiCall(
            call = { api.getToken(clientSecret = clientSecret, deviceCode = deviceCode) },
            errorMessage = "Error Fetching Token"
        )

        return authResponse;

    }
}
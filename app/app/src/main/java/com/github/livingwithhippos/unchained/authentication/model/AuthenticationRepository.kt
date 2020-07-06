package com.github.livingwithhippos.unchained.authentication.model

import com.github.livingwithhippos.unchained.base.model.repositories.BaseRepository
import com.github.livingwithhippos.unchained.base.network.AuthApiHelper
import javax.inject.Inject

class AuthenticationRepository @Inject constructor (private val apiHelper: AuthApiHelper) : BaseRepository() {

    suspend fun getVerificationCode(): Authentication? {

        val authResponse = safeApiCall(
            call = { apiHelper.getAuthentication() },
            errorMessage = "Error Fetching Authentication Info"
        )

        return authResponse;

    }

    suspend fun getSecrets(code: String): Secrets? {

        val authResponse = safeApiCall(
            call = { apiHelper.getSecrets(deviceCode = code) },
            errorMessage = "Error Fetching Secrets"
        )

        return authResponse;

    }

    suspend fun getToken(clientId: String, clientSecret: String, deviceCode: String): Token? {

        val authResponse = safeApiCall(
            call = { apiHelper.getToken(clientId = clientId, clientSecret = clientSecret, deviceCode = deviceCode) },
            errorMessage = "Error Fetching Token"
        )

        return authResponse;

    }
}
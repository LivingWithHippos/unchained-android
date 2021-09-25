package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelper
import javax.inject.Inject

class AuthenticationRepository @Inject constructor(private val apiHelper: AuthApiHelper) :
    BaseRepository() {

    suspend fun getVerificationCode(): Authentication? {

        val authResponse = safeApiCall(
            call = { apiHelper.getAuthentication() },
            errorMessage = "Error Fetching Authentication Info"
        )

        return authResponse
    }

    suspend fun getSecrets(code: String): Secrets? {

        val secretResponse = safeApiCall(
            call = { apiHelper.getSecrets(deviceCode = code) },
            errorMessage = "Error Fetching Secrets"
        )

        return secretResponse
    }

    suspend fun getToken(clientId: String, clientSecret: String, code: String): Token? {

        val tokenResponse = safeApiCall(
            call = {
                apiHelper.getToken(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    code = code
                )
            },
            errorMessage = "Error Fetching Token"
        )

        return tokenResponse
    }

    /**
     * Get a new open source Token that usually lasts one hour.
     * @param clientId the client id obtained from the /device/credentials endpoint
     * @param clientSecret the code obtained from the /token endpoint
     * @param refreshToken the device code obtained from the /device/code endpoint
     * @return the new Token
     */
    suspend fun refreshToken(clientId: String, clientSecret: String, refreshToken: String): Token? =
        getToken(clientId, clientSecret, refreshToken)

    suspend fun refreshToken(credentials: Credentials.CurrentCredential): Token? =
        refreshToken(credentials.clientId!!, credentials.clientSecret!!, credentials.refreshToken!!)
}

package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.authentication.model.Authentication
import com.github.livingwithhippos.unchained.authentication.model.Secrets
import com.github.livingwithhippos.unchained.authentication.model.Token
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import com.github.livingwithhippos.unchained.base.model.network.AuthApiHelper
import com.github.livingwithhippos.unchained.base.model.network.Host
import javax.inject.Inject

class AuthenticationRepository @Inject constructor(private val apiHelper: AuthApiHelper) :
    BaseRepository() {

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
            call = {
                apiHelper.getToken(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    deviceCode = deviceCode
                )
            },
            errorMessage = "Error Fetching Token"
        )

        return authResponse;

    }

    suspend fun disableToken(token: String): Any? {

        val authResponse = safeApiCall(
            call = { apiHelper.disableToken("Bearer $token") },
            errorMessage = "Error disabling token"
        )

        return authResponse;

    }


    /**
     * Get a new open source Token that usually lasts one hour.
     * You can not use both offset and page at the same time, page is prioritized in case it happens.
     * @param clientId the client id obtained from the /device/credentials endpoint
     * @param refreshCode the code obtained from the /token endpoint
     * @param deviceCode the device code obtained from the /device/code endpoint
     * @return the new Token
     */
    suspend fun refreshToken(clientId: String, refreshCode: String, deviceCode: String): Token?  = getToken(clientId, refreshCode, deviceCode)
    suspend fun refreshToken(credentials: Credentials): Token? = refreshToken(credentials.clientId!!, credentials.refreshToken!!, credentials.deviceCode)
}
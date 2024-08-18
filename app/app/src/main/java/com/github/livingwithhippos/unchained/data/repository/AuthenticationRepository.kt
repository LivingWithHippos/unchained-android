package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject

class AuthenticationRepository
@Inject
constructor(private val protoStore: ProtoStore, private val apiHelper: AuthApiHelper) :
    BaseRepository(protoStore) {

    suspend fun getVerificationCode(): Authentication? {

        val authResponse =
            safeApiCall(
                call = { apiHelper.getAuthentication() },
                errorMessage = "Error Fetching Authentication Info")

        return authResponse
    }

    suspend fun getSecrets(code: String): Secrets? {

        val secretResponse =
            safeApiCall(
                call = { apiHelper.getSecrets(deviceCode = code) },
                errorMessage = "Error Fetching Secrets")

        return secretResponse
    }

    suspend fun getToken(clientId: String, clientSecret: String, code: String): Token? {

        val tokenResponse =
            safeApiCall(
                call = {
                    apiHelper.getToken(
                        clientId = clientId, clientSecret = clientSecret, code = code)
                },
                errorMessage = "Error Fetching Token")

        return tokenResponse
    }

    suspend fun getTokenOrError(
        clientId: String,
        clientSecret: String,
        code: String
    ): EitherResult<UnchainedNetworkException, Token> {

        val tokenResponse =
            eitherApiResult(
                call = {
                    apiHelper.getToken(
                        clientId = clientId, clientSecret = clientSecret, code = code)
                },
                errorMessage = "Error Fetching Token")

        return tokenResponse
    }

    /**
     * Get a new open source Token that usually lasts one hour.
     *
     * @param clientId the client id obtained from the /device/credentials endpoint
     * @param clientSecret the code obtained from the /token endpoint
     * @param refreshToken the device code obtained from the /device/code endpoint
     * @return the new Token
     */
    suspend fun refreshToken(clientId: String, clientSecret: String, refreshToken: String): Token? =
        getToken(clientId, clientSecret, refreshToken)

    suspend fun refreshTokenWithError(
        credentials: com.github.livingwithhippos.unchained.data.local.Credentials.CurrentCredential
    ): EitherResult<UnchainedNetworkException, Token> =
        getTokenOrError(
            credentials.clientId!!, credentials.clientSecret!!, credentials.refreshToken!!)
}

package com.github.livingwithhippos.unchained.base.model.network

import com.github.livingwithhippos.unchained.authentication.model.Authentication
import com.github.livingwithhippos.unchained.authentication.model.AuthenticationApi
import com.github.livingwithhippos.unchained.authentication.model.Secrets
import com.github.livingwithhippos.unchained.authentication.model.Token
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelper
import retrofit2.Response
import javax.inject.Inject

class AuthApiHelperImpl @Inject constructor(private val authenticationApi: AuthenticationApi) :
    AuthApiHelper {

    override suspend fun getAuthentication(): Response<Authentication> =
        authenticationApi.getAuthentication()

    override suspend fun getSecrets(deviceCode: String): Response<Secrets> =
        authenticationApi.getSecrets(deviceCode = deviceCode)

    override suspend fun getToken(
        clientId: String,
        clientSecret: String,
        deviceCode: String
    ): Response<Token> = authenticationApi.getToken(clientId, clientSecret, deviceCode)

    override suspend fun disableToken(token: String): Response<Any> =
        authenticationApi.disableToken(token)
}
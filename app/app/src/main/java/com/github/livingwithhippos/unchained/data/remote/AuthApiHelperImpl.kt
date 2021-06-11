package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
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
        code: String
    ): Response<Token> = authenticationApi.getToken(clientId, clientSecret, code)
}

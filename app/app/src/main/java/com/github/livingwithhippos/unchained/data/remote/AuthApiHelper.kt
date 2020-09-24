package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.authentication.model.Authentication
import com.github.livingwithhippos.unchained.authentication.model.Secrets
import com.github.livingwithhippos.unchained.authentication.model.Token
import retrofit2.Response

interface AuthApiHelper {

    suspend fun getAuthentication(): Response<Authentication>

    suspend fun getSecrets(
        deviceCode: String
    ): Response<Secrets>

    suspend fun getToken(
        clientId: String,
        clientSecret: String,
        deviceCode: String
    ): Response<Token>

    suspend fun disableToken(
        token: String
    ): Response<Any>
}
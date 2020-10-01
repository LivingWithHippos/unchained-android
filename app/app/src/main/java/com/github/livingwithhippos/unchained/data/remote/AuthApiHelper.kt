package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
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
}
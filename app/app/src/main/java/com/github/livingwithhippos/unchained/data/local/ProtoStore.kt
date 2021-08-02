package com.github.livingwithhippos.unchained.data.local

import kotlinx.coroutines.flow.Flow

interface ProtoStore {

    val credentialsFlow: Flow<Credentials.CurrentCredential>

    suspend fun setCredentials(
        deviceCode: String,
        clientId: String? = null,
        clientSecret: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null
    )

    suspend fun updateCredentials(
        deviceCode: String? = null,
        clientId: String? = null,
        clientSecret: String? = null,
        accessToken: String? = null,
        refreshToken: String? = null
    )
}
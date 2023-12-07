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

    suspend fun updateDeviceCode(deviceCode: String)

    suspend fun updateClientId(clientId: String)

    suspend fun updateClientSecret(clientSecret: String)

    suspend fun updateAccessToken(accessToken: String)

    suspend fun updateRefreshToken(refreshToken: String)

    suspend fun deleteCredentials()

    suspend fun deleteIncompleteCredentials()

    suspend fun getCredentials(): Credentials.CurrentCredential
}

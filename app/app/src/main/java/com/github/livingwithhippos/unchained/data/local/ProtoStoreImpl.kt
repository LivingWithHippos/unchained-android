package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import javax.inject.Inject


class ProtoStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ProtoStore {
    override val credentialsFlow: Flow<Credentials.CurrentCredential>
        get() = context.credentialsDataStore.data.catch { exception ->
            if (exception is IOException) {
                exception.printStackTrace()
                emit(Credentials.CurrentCredential.getDefaultInstance())
            } else {
                throw exception
            }
        }

    override suspend fun setCredentials(
        deviceCode: String,
        clientId: String?,
        clientSecret: String?,
        accessToken: String?,
        refreshToken: String?
    ) {
        context.credentialsDataStore.updateData { credentials ->
            credentials.toBuilder()
                .setDeviceCode(deviceCode)
                .setAccessToken(accessToken)
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build()
        }
    }

    override suspend fun updateCredentials(
        deviceCode: String?,
        clientId: String?,
        clientSecret: String?,
        accessToken: String?,
        refreshToken: String?
    ) {
        context.credentialsDataStore.updateData { credentials ->
            val builder = credentials.toBuilder()
            if (!deviceCode.isNullOrBlank())
                builder.deviceCode = deviceCode
            if (!accessToken.isNullOrBlank())
                builder.accessToken = accessToken
            if (!clientId.isNullOrBlank())
                builder.clientId = clientId
            if (!clientSecret.isNullOrBlank())
                builder.clientSecret = clientSecret
            if (!refreshToken.isNullOrBlank())
                builder.refreshToken = refreshToken
            builder.build()
        }
    }

    override suspend fun deleteCredentials() {
        context.credentialsDataStore.updateData { it.toBuilder().clear().build() }
    }

    override suspend fun deleteIncompleteCredentials() {
        context.credentialsDataStore.updateData {
            if (
                it.deviceCode.isNullOrBlank() ||
                it.accessToken.isNullOrBlank() ||
                it.clientId.isNullOrBlank()||
                it.clientSecret.isNullOrBlank()||
                it.refreshToken.isNullOrBlank()
            )
                it.toBuilder().clear().build()
            else {
                // todo: a smarter way to remove an incomplete element?
                // get the original one and call deleteCredentials() checking there instead of here?
                it.toBuilder().build()
            }
        }
    }


}
package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

class ProtoStoreImpl @Inject constructor(@ApplicationContext private val context: Context) :
    ProtoStore {

    override val credentialsFlow: Flow<Credentials.CurrentCredential> =
        context.credentialsDataStore.data.catch { exception ->
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
            credentials
                .toBuilder()
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
            if (!deviceCode.isNullOrBlank()) builder.deviceCode = deviceCode
            if (!accessToken.isNullOrBlank()) builder.accessToken = accessToken
            if (!clientId.isNullOrBlank()) builder.clientId = clientId
            if (!clientSecret.isNullOrBlank()) builder.clientSecret = clientSecret
            if (!refreshToken.isNullOrBlank()) builder.refreshToken = refreshToken
            builder.build()
        }
    }

    override suspend fun updateDeviceCode(deviceCode: String) {
        context.credentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setDeviceCode(deviceCode).build()
        }
    }

    override suspend fun updateClientId(clientId: String) {
        context.credentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setClientId(clientId).build()
        }
    }

    override suspend fun updateClientSecret(clientSecret: String) {
        context.credentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setClientSecret(clientSecret).build()
        }
    }

    override suspend fun updateAccessToken(accessToken: String) {
        context.credentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setAccessToken(accessToken).build()
        }
    }

    override suspend fun updateRefreshToken(refreshToken: String) {
        context.credentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setRefreshToken(refreshToken).build()
        }
    }

    override suspend fun deleteCredentials() {
        context.credentialsDataStore.updateData { it.toBuilder().clear().build() }
    }

    override suspend fun deleteIncompleteCredentials() {
        val credentials = getCredentials()
        if (
            credentials.deviceCode.isNullOrBlank() ||
                credentials.accessToken.isNullOrBlank() ||
                credentials.clientId.isNullOrBlank() ||
                credentials.clientSecret.isNullOrBlank() ||
                credentials.refreshToken.isNullOrBlank()
        ) {
            context.credentialsDataStore.updateData { it.toBuilder().clear().build() }
        }
    }

    override suspend fun getCredentials(): Credentials.CurrentCredential {
        return try {
            credentialsFlow.first()
        } catch (e: Exception) {
            e.printStackTrace()
            Credentials.CurrentCredential.getDefaultInstance()
        }
    }
}

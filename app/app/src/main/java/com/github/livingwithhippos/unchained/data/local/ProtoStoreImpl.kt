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


}
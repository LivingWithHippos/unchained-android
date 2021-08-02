package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.github.livingwithhippos.unchained.data.local.Credentials.CurrentCredential
import java.io.InputStream
import java.io.OutputStream

object CredentialsSerializer : Serializer<CurrentCredential> {
    override val defaultValue: CurrentCredential
        get() = CurrentCredential.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CurrentCredential {
        try {
            return CurrentCredential.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    }

    override suspend fun writeTo(t: CurrentCredential, output: OutputStream) = t.writeTo(output)

}

val Context.credentialsDataStore: DataStore<CurrentCredential> by dataStore(
    fileName = "credentials.pb",
    serializer = CredentialsSerializer
)
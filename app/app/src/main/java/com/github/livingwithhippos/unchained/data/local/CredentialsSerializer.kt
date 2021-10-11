package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.github.livingwithhippos.unchained.data.local.Credentials.CurrentCredential
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object CredentialsSerializer : Serializer<CurrentCredential> {
    override val defaultValue: CurrentCredential = CurrentCredential.getDefaultInstance()

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun readFrom(input: InputStream): CurrentCredential {
        try {
            return CurrentCredential.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun writeTo(t: CurrentCredential, output: OutputStream) = t.writeTo(output)
}

val Context.credentialsDataStore: DataStore<CurrentCredential> by dataStore(
    fileName = "credentials.pb",
    serializer = CredentialsSerializer
)
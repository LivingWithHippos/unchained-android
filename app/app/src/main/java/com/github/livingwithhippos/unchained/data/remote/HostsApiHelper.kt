package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Host
import retrofit2.Response

interface HostsApiHelper {
    suspend fun getHostsStatus(token: String): Response<Host>

    suspend fun getHostsRegex(): Response<List<String>>

    suspend fun getHostsFoldersRegex(): Response<List<String>>
}

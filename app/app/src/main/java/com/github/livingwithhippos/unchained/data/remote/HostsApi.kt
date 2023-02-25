package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Host
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface HostsApi {

    @GET("hosts/status/")
    suspend fun getHostsStatus(@Header("Authorization") token: String): Response<Host>

    @GET("hosts/regex/") suspend fun getHostsRegex(): Response<List<String>>

    @GET("hosts/regexFolder/") suspend fun getHostsFoldersRegex(): Response<List<String>>
}

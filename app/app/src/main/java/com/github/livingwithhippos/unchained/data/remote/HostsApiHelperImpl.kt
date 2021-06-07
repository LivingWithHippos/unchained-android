package com.github.livingwithhippos.unchained.data.remote


import com.github.livingwithhippos.unchained.data.model.Host
import retrofit2.Response
import javax.inject.Inject

class HostsApiHelperImpl @Inject constructor(private val hostsApi: HostsApi) :
    HostsApiHelper {

    override suspend fun getHostsStatus(token: String): Response<Host> = hostsApi.getStreams(token)

    override suspend fun getHostsRegex(): Response<List<String>> = hostsApi.getHostsRegex()

    override suspend fun getHostsFoldersRegex(): Response<List<String>> =
        hostsApi.getHostsFoldersRegex()
}
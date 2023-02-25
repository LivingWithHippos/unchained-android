package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Host
import javax.inject.Inject
import retrofit2.Response

class HostsApiHelperImpl @Inject constructor(private val hostsApi: HostsApi) : HostsApiHelper {

    override suspend fun getHostsStatus(token: String): Response<Host> =
        hostsApi.getHostsStatus(token)

    override suspend fun getHostsRegex(): Response<List<String>> = hostsApi.getHostsRegex()

    override suspend fun getHostsFoldersRegex(): Response<List<String>> =
        hostsApi.getHostsFoldersRegex()
}

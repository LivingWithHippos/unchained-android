package com.github.livingwithhippos.unchained.data.remote

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.data.model.Host
import com.github.livingwithhippos.unchained.utilities.DebridProvider
import com.github.livingwithhippos.unchained.utilities.getDebridProvider
import javax.inject.Inject
import retrofit2.Response

class HostsApiHelperImpl
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val hostsApi: HostsApi,
    private val allDebridTorrentsApi: AllDebridTorrentsApi,
) : HostsApiHelper {

    override suspend fun getHostsStatus(token: String): Response<Host> =
        hostsApi.getHostsStatus(token)

    override suspend fun getHostsRegex(): Response<List<String>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> hostsApi.getHostsRegex()
            DebridProvider.AllDebrid -> {
                val response = allDebridTorrentsApi.getHosts()
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val data = body?.data
                    if (body?.status == "success" && data != null) {
                        Response.success(data.toRegexList())
                    } else allDebridErrorResponse(body?.error)
                }
            }
        }

    override suspend fun getHostsFoldersRegex(): Response<List<String>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> hostsApi.getHostsFoldersRegex()
            DebridProvider.AllDebrid -> Response.success(emptyList())
        }
}

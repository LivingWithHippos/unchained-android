package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.base.model.network.Host
import retrofit2.Response

interface HostsApiHelper {
    suspend fun getHostsStatus(token: String): Response<Host>
}
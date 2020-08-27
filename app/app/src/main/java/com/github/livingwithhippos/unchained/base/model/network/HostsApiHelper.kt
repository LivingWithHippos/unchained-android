package com.github.livingwithhippos.unchained.base.model.network

import retrofit2.Response

interface HostsApiHelper {
    suspend fun getHostsStatus(token: String): Response<Host>
}
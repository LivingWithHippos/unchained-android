package com.github.livingwithhippos.unchained.data.remote

import retrofit2.Response

interface KodiApiHelper {
    suspend fun openUrl(request: KodiOpenRequest): Response<KodiResponse>
}
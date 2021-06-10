package com.github.livingwithhippos.unchained.data.remote

import retrofit2.Response
import com.github.livingwithhippos.unchained.data.model.KodiOpenRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse

interface KodiApiHelper {
    suspend fun openUrl(request: KodiOpenRequest): Response<KodiResponse>
}
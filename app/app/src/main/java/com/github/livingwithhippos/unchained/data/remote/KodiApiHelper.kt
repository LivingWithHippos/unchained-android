package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.KodiGenericResponse
import com.github.livingwithhippos.unchained.data.model.KodiRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import retrofit2.Response

interface KodiApiHelper {
    suspend fun openUrl(request: KodiRequest, auth: String?): Response<KodiResponse>

    suspend fun getVolume(request: KodiRequest, auth: String?): Response<KodiGenericResponse>
}

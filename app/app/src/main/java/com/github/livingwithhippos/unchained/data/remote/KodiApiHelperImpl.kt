package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.KodiGenericResponse
import com.github.livingwithhippos.unchained.data.model.KodiRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import retrofit2.Response


class KodiApiHelperImpl(private val kodiApi: KodiApi) : KodiApiHelper {
    override suspend fun openUrl(request: KodiRequest): Response<KodiResponse> =
        kodiApi.openUrl(request)

    override suspend fun getVolume(request: KodiRequest): Response<KodiGenericResponse>  =
        kodiApi.getVolume(request)
}

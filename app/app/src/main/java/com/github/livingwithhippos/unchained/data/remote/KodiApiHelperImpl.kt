package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.KodiGenericResponse
import com.github.livingwithhippos.unchained.data.model.KodiRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import retrofit2.Response

class KodiApiHelperImpl(private val kodiApi: KodiApi) : KodiApiHelper {
    override suspend fun openUrl(request: KodiRequest, auth: String?): Response<KodiResponse> =
        kodiApi.openUrl(request, auth)

    override suspend fun getVolume(request: KodiRequest, auth: String?): Response<KodiGenericResponse> =
        kodiApi.getVolume(request, auth)
}

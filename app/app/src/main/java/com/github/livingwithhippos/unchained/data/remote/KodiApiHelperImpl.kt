package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.KodiOpenRequest
import com.github.livingwithhippos.unchained.data.model.KodiResponse
import retrofit2.Response
import javax.inject.Inject


class KodiApiHelperImpl @Inject constructor(private val kodiApi: KodiApi) : KodiApiHelper {
    override suspend fun openUrl(request: KodiOpenRequest): Response<KodiResponse> =
        kodiApi.openUrl(request)
}

package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Updates
import retrofit2.Response

class UpdateApiHelperImpl (private val updateApi: UpdateApi) : UpdateApiHelper {
    override suspend fun getUpdates(url: String): Response<Updates> = updateApi.getUpdates(url)
}

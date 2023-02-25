package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Updates
import javax.inject.Inject
import retrofit2.Response

class UpdateApiHelperImpl @Inject constructor(private val updateApi: UpdateApi) : UpdateApiHelper {
    override suspend fun getUpdates(url: String): Response<Updates> = updateApi.getUpdates(url)
}

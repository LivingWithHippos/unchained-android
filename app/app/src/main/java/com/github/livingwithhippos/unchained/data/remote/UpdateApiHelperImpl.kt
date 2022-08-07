package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Updates
import retrofit2.Response
import javax.inject.Inject

class UpdateApiHelperImpl @Inject constructor(private val updateApi: UpdateApi) :
    UpdateApiHelper {
    override suspend fun getUpdates(url: String): Response<Updates> = updateApi.getUpdates(url)
}

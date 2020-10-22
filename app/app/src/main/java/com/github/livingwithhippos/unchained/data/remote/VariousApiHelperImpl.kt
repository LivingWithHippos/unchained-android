package com.github.livingwithhippos.unchained.data.remote

import retrofit2.Response
import javax.inject.Inject

class VariousApiHelperImpl @Inject constructor(private val variousApi: VariousApi) :
    VariousApiHelper {
    override suspend fun disableToken(token: String): Response<Unit> =
        variousApi.disableToken(token)
}
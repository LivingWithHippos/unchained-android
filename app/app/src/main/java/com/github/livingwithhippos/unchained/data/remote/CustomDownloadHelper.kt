package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.search.model.RemotePlugin
import okhttp3.ResponseBody
import retrofit2.Response

interface CustomDownloadHelper {

    suspend fun getFile(url: String): Response<ResponseBody>

    suspend fun getPluginsPack(
        packUrl: String
    ): Response<ResponseBody>

    suspend fun getPluginsRepository(
        repositoryUrl: String
    ): Response<List<RemotePlugin>>
}

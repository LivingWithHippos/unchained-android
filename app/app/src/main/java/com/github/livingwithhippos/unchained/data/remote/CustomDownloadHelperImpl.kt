package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.search.model.RemotePlugin
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class CustomDownloadHelperImpl @Inject constructor(private val customDownload: CustomDownload) :
    CustomDownloadHelper {
    override suspend fun getFile(url: String): Response<ResponseBody> = customDownload.getFile(url)

    override suspend fun getPluginsPack(packUrl: String): Response<ResponseBody> =
        customDownload.getPluginsPack(packUrl)

    override suspend fun getPluginsRepository(repositoryUrl: String): Response<List<RemotePlugin>> =
        customDownload.getPluginsRepository(repositoryUrl)
}

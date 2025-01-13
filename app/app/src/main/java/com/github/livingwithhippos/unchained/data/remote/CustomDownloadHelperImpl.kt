package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.repository.model.JsonPluginRepository
import okhttp3.ResponseBody
import retrofit2.Response

class CustomDownloadHelperImpl(private val customDownload: CustomDownload) :
    CustomDownloadHelper {
    override suspend fun getFile(url: String): Response<ResponseBody> = customDownload.getFile(url)

    override suspend fun getPluginsRepository(
        repositoryUrl: String
    ): Response<JsonPluginRepository> = customDownload.getPluginsRepository(repositoryUrl)

    override suspend fun getPlugin(pluginUrl: String): Response<Plugin> =
        customDownload.getPlugin(pluginUrl)

    override suspend fun getAsString(url: String): Response<String> = customDownload.getString(url)
}

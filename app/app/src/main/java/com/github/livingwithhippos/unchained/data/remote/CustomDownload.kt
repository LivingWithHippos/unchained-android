package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.repository.model.JsonPluginRepository
import com.github.livingwithhippos.unchained.utilities.PLUGINS_PACK_LINK
import com.github.livingwithhippos.unchained.utilities.DEFAULT_PLUGINS_REPOSITORY_LINK
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface CustomDownload {

    @Streaming
    @GET
    suspend fun getPluginsPack(
        @Url packUrl: String = PLUGINS_PACK_LINK
    ): Response<ResponseBody>

    @Streaming
    @GET
    suspend fun getFile(
        @Url url: String
    ): Response<ResponseBody>

    @GET
    suspend fun getPluginsRepository(
        @Url repositoryUrl: String = DEFAULT_PLUGINS_REPOSITORY_LINK
    ): Response<JsonPluginRepository>

    @GET
    suspend fun getPlugin(
        @Url pluginUrl: String
    ): Response<Plugin>
}

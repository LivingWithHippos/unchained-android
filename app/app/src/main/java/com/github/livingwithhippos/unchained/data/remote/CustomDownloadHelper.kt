package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.repository.model.JsonPluginRepository
import okhttp3.ResponseBody
import retrofit2.Response

interface CustomDownloadHelper {

    suspend fun getFile(url: String): Response<ResponseBody>

    suspend fun getPluginsRepository(repositoryUrl: String): Response<JsonPluginRepository>

    suspend fun getPlugin(pluginUrl: String): Response<Plugin>

    suspend fun getAsString(url: String): Response<String>
}

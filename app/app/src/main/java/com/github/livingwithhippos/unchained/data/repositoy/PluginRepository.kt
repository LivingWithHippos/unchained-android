package com.github.livingwithhippos.unchained.data.repositoy

import android.content.Context
import com.github.livingwithhippos.unchained.data.local.AssetsManager
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.utilities.extension.smartList
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class PluginRepository @Inject constructor(
    private val assetsManager: AssetsManager
) {


    //todo: inject
    private val pluginAdapter: JsonAdapter<Plugin> = Moshi.Builder()
        .build()
        .adapter(Plugin::class.java)

    suspend fun getPlugins(context: Context): List<Plugin> = withContext(Dispatchers.IO) {

        /**
         * get local json files from the assets folder
         */
        val jsonFiles = assetsManager.searchFiles(TYPE_JSON, PLUGIN_FOLDER)
        val plugins = mutableListOf<Plugin>()

        for (json in jsonFiles) {

            val plugin: Plugin? = try {
                val pluginJSON = context.assets.open(json)
                    .bufferedReader()
                    .use { it.readText() }

                pluginAdapter.fromJson(pluginJSON)
            } catch (ex: Exception) {
                Timber.e("Error reading file in path $json, exception ${ex.message}")
                null
            }

            if (plugin != null)
                plugins.add(plugin)
        }

        /**
         * get installed .unchained search plugins
         */

        context.fileList().filter {
            it.endsWith(TYPE_UNCHAINED)
        }.forEach {
            context.openFileInput(it).bufferedReader().use { reader ->
                try {
                    val plugin = pluginAdapter.fromJson(reader.readText())
                    if (plugin != null)
                        plugins.add(plugin)
                } catch (ex: Exception) {
                    Timber.e("Error reading file in path $it, exception ${ex.message}")
                }
            }


        }

        plugins
    }

    fun removeExternalPlugins(context: Context): Int {
        return try {
            val plugins = context.filesDir.listFiles { _, name ->
                name.endsWith(TYPE_UNCHAINED)
            }

            plugins?.forEach {
                it.delete()
            }

            plugins?.size ?: -1

        } catch (e: SecurityException) {
            Timber.d("Security exception deleting plugins files: ${e.message}")
            -1
        }
    }

    companion object {
        const val PLUGIN_FOLDER = "search_plugins"

        // todo: rename in .unchained and add it to the manifest
        const val TYPE_JSON = ".json"
        const val TYPE_UNCHAINED = ".unchained"
    }
}
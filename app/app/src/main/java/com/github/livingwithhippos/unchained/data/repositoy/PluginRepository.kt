package com.github.livingwithhippos.unchained.data.repositoy

import android.content.Context
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
    @ApplicationContext private val appContext: Context,
) {

    private val SYSTEM_ASSETS_FOLDER = listOf("images", "webkit")

    //todo: inject
    private val pluginAdapter: JsonAdapter<Plugin> = Moshi.Builder()
        .build()
        .adapter(Plugin::class.java)

    private fun getAssetsPath(path: String): Array<String> {
        // on some phones the path works with / at the end, on others not.
        return appContext.assets.smartList(path) ?: emptyArray()
    }

    // TODO: check if this returns true also for empty directories
    fun isFile(path: String): Boolean {
        return appContext.assets.list(path).isNullOrEmpty()
    }

    suspend fun getPlugins(): List<Plugin> = withContext(Dispatchers.IO) {

        val jsonFiles = searchFiles()
        val plugins = mutableListOf<Plugin>()

        for (json in jsonFiles) {
            try {
                /**
                 * parse
                 * add to list
                 */

                val pluginJSON = appContext.assets.open(json)
                    .bufferedReader()
                    .use { it.readText() }

                val plugin = pluginAdapter.fromJson(pluginJSON)
                if (plugin != null)
                    plugins.add(plugin)

            } catch (ex: Exception) {
                Timber.e("Error reading file in path $json, exception ${ex.message}")
            }
        }

        plugins
    }

    fun searchFiles(
        fileType: String = TYPE_JSON,
        folder: String = PLUGIN_FOLDER,
        skipSystemFolders: Boolean = true
    ): List<String> {
        val results: MutableList<String> = mutableListOf()
        val pathList = getAssetsPath(folder)

        for (path in pathList) {
            // skip if system folder
            if (skipSystemFolders && SYSTEM_ASSETS_FOLDER.contains(path))
                continue

            val newPath = if (folder == "") path else folder.plus("/").plus(path)
            // the path is a file
            if (isFile(newPath)) {
                // if the name is correct we add it to the list
                if (newPath.endsWith(fileType))
                    results.add(newPath)
            }
        }

        return results
    }

    companion object {
        const val PLUGIN_FOLDER = "search_plugins"

        // todo: rename in .unchained and add it to the manifest
        const val TYPE_JSON = ".json"
    }
}
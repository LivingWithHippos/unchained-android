package com.github.livingwithhippos.unchained.data.repository

import android.content.Context
import android.net.Uri
import com.github.livingwithhippos.unchained.data.local.AssetsManager
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

class PluginRepository @Inject constructor(
    private val assetsManager: AssetsManager
) {

    // todo: inject
    private val pluginAdapter: JsonAdapter<Plugin> = Moshi.Builder()
        .build()
        .adapter(Plugin::class.java)

    suspend fun getPlugins(context: Context): Pair<List<Plugin>, Int> =
        withContext(Dispatchers.IO) {

            /**
             * get local .json and .unchained files from the search_plugin folder in the assets folder
             */
            val jsonFiles = assetsManager.searchFiles(TYPE_JSON, PLUGIN_FOLDER)
            val unchainedFiles = assetsManager.searchFiles(TYPE_UNCHAINED, PLUGIN_FOLDER)
            val plugins = mutableListOf<Plugin>()

            var errors = 0

            for (json in jsonFiles) {

                val plugin: Plugin? = getPluginFromPath(context, json)

                if (plugin != null)
                    plugins.add(plugin)
                else
                    errors++
            }

            for (json in unchainedFiles) {

                val plugin: Plugin? = getPluginFromPath(context, json)

                if (plugin != null)
                    plugins.add(plugin)
            }

            /**
             * get installed .unchained search plugins
             */

            context.fileList().filter {
                it.endsWith(TYPE_UNCHAINED, ignoreCase = true)
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

            Pair(plugins, errors)
        }

    private fun getPluginFromJSON(json: String): Plugin? {
        return try {
            pluginAdapter.fromJson(json)
        } catch (ex: Exception) {
            Timber.e("Error reading json string, exception ${ex.message}")
            null
        }
    }

    private fun getPluginFromPath(context: Context, json: String): Plugin? {
        return try {
            val pluginJSON = context.assets.open(json)
                .bufferedReader()
                .use { it.readText() }

            pluginAdapter.fromJson(pluginJSON)
        } catch (ex: Exception) {
            Timber.e("Error reading file in path $json, exception ${ex.message}")
            null
        }
    }

    fun getExternalPlugin(context: Context, filename: String): Plugin? {
        val file = File(context.filesDir, filename)
        return getPluginFromJSON(file.readText())
    }

    fun removeExternalPlugins(context: Context): Int {
        return try {
            val plugins = context.filesDir.listFiles { _, name ->
                name.endsWith(TYPE_UNCHAINED, ignoreCase = true)
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

    fun getPluginsNames(context: Context): List<String> {
        val names = mutableListOf<String>()
        names.addAll(
            assetsManager.searchFiles(TYPE_JSON, PLUGIN_FOLDER)
                .map { it.split("/").last() }
        )
        names.addAll(
            context.fileList().filter {
                it.endsWith(TYPE_UNCHAINED, ignoreCase = true)
            }.map { it.split("/").last() }
        )
        return names
    }

    suspend fun addExternalPlugin(
        context: Context,
        data: Uri,
        customFileName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val filename = customFileName ?: data.path?.replace("%2F", "/")?.split("/")?.last()
        if (filename != null) {
            // save the file locally
            return@withContext try {
                context.contentResolver.openInputStream(data)?.use { inputStream ->
                    val buffer: ByteArray = inputStream.readBytes()
                    context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                        it.write(buffer)
                    }
                }
                true
            } catch (exception: Exception) {
                Timber.e("Error loading the file ${data.path}: ${exception.message}")
                false
            }
        }
        return@withContext true
    }

    suspend fun addExternalPlugin(context: Context, source: String): Boolean =
        withContext(Dispatchers.IO) {

            val plugin: Plugin? = getPluginFromJSON(source)

            if (plugin != null) {
                val filename = plugin.name + TYPE_UNCHAINED
                try {
                    context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                        val buffer: ByteArray = source.toByteArray()
                        it.write(buffer)
                    }
                    return@withContext true
                } catch (exception: IOException) {
                    Timber.e("Error adding the plugin $filename: ${exception.message}")
                    false
                }
            } else
                return@withContext false
        }

    suspend fun readPassedPlugin(context: Context, data: Uri): Plugin? =
        withContext(Dispatchers.IO) {

            val filename = data.path?.split("/")?.last()
            if (filename != null) {

                try {
                    context.contentResolver.openInputStream(data)?.use { inputStream ->
                        val json = inputStream.bufferedReader().readText()
                        return@withContext getPluginFromJSON(json)
                    }
                } catch (exception: Exception) {
                    Timber.e("Error adding the plugin $filename: ${exception.message}")
                }
            }

            null
        }

    companion object {
        const val PLUGIN_FOLDER = "search_plugins"

        // todo: rename in .unchained and add it to the manifest
        const val TYPE_JSON = ".json"
        const val TYPE_UNCHAINED = ".unchained"
    }
}

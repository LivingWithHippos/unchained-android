package com.github.livingwithhippos.unchained.data.repository

import android.content.Context
import android.net.Uri
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.repository.model.RepositoryListItem
import com.github.livingwithhippos.unchained.utilities.MANUAL_PLUGINS_REPOSITORY_NAME
import com.github.livingwithhippos.unchained.utilities.getManualPluginFilename
import com.github.livingwithhippos.unchained.utilities.getPluginFilename
import com.github.livingwithhippos.unchained.utilities.getRepositoryString
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PluginRepository @Inject constructor() {

    // todo: inject
    private val pluginAdapter: JsonAdapter<Plugin> =
        Moshi.Builder().build().adapter(Plugin::class.java)

    suspend fun getPlugins(
        context: Context,
        manuallyInstalledOnly: Boolean = false
    ): Pair<List<Plugin>, Int> =
        withContext(Dispatchers.IO) {
            val pluginFiles = mutableListOf<File>()
            val pluginFolder = context.getDir("plugins", Context.MODE_PRIVATE)
            if (pluginFolder.exists()) {
                if (manuallyInstalledOnly) {
                    val localPluginFolder = File(pluginFolder, MANUAL_PLUGINS_REPOSITORY_NAME)
                    if (localPluginFolder.exists()) {
                        pluginFolder.walk().forEach {
                            if (it.isFile && it.name.endsWith(TYPE_UNCHAINED, ignoreCase = true)) {
                                pluginFiles.add(it)
                            }
                        }
                    }
                } else {
                    pluginFolder.walk().forEach {
                        if (it.isFile && it.name.endsWith(TYPE_UNCHAINED, ignoreCase = true)) {
                            pluginFiles.add(it)
                        }
                    }
                }
            }

            val plugins = mutableListOf<Plugin>()

            var errors = 0

            for (file in pluginFiles) {
                try {
                    val json = file.readText()
                    val plugin: Plugin? = pluginAdapter.fromJson(json)
                    if (plugin != null) plugins.add(plugin) else errors++
                } catch (ex: Exception) {
                    Timber.e("Exception while parsing json plugin: $ex")
                }
            }

            Pair(plugins, errors)
        }

    suspend fun getPluginsWithFolders(context: Context): LocalPlugins =
        withContext(Dispatchers.IO) {

            /** list of plugin files associated with the repository folder name */

            // note: the actual folder name may be different from the one put here. For example it
            // could
            // get changed to "app_plugins"
            // it is still deterministic
            val pluginFolder = context.getDir("plugins", Context.MODE_PRIVATE)
            val pluginRepoFileAssociation = mutableMapOf<String, List<File>>()
            if (pluginFolder.exists()) {
                val files = mutableListOf<File>()
                var repoFolder = ""
                pluginFolder.walk().forEachIndexed { index, currentFile ->
                    // skip the first folder, which is the "plugins" folder itself
                    if (index > 0) {
                        if (currentFile.isDirectory) {
                            if (repoFolder != "") {
                                // finished scanning a directory and passing to a new one
                                pluginRepoFileAssociation[repoFolder] = files
                                // todo: check if this changes the saved one
                                files.clear()
                            }
                            repoFolder = currentFile.name
                            Timber.d("Found folder $repoFolder")
                        } else if (
                            currentFile.isFile &&
                                currentFile.name.endsWith(TYPE_UNCHAINED, ignoreCase = true)
                        ) {
                            Timber.d("Found plugin ${currentFile.name}")
                            files.add(currentFile)
                        } else {
                            Timber.w("Unknown file found: ${currentFile.name}")
                        }
                    }
                }
                // we don't pass through the last save in the forEach
                if (files.isNotEmpty() && repoFolder != "") {
                    pluginRepoFileAssociation[repoFolder] = files
                }
            }

            if (pluginRepoFileAssociation.isEmpty()) {
                Timber.d("No plugins found")
                return@withContext LocalPlugins(emptyMap(), 0)
            }

            var errors = 0

            val pluginsData = mutableMapOf<String, List<Plugin>>()

            pluginRepoFileAssociation.keys.forEach { repoName ->
                val repoList = mutableListOf<Plugin>()
                pluginRepoFileAssociation[repoName]?.forEach { pluginFile ->
                    try {
                        val json = pluginFile.readText()
                        val plugin: Plugin? = pluginAdapter.fromJson(json)
                        if (plugin != null) repoList.add(plugin) else errors++
                    } catch (ex: Exception) {
                        Timber.e("Exception while parsing json plugin: $ex")
                        errors++
                    }
                }
                pluginsData[repoName] = repoList
            }

            LocalPlugins(pluginsData, errors)
        }

    suspend fun removePlugin(
        context: Context,
        repository: String,
        author: String?,
        name: String
    ): Boolean =
        withContext(Dispatchers.IO) {
            val pluginFolder = context.getDir("plugins", Context.MODE_PRIVATE)
            val repoName = getRepositoryString(repository)
            val filename =
                if (repository == MANUAL_PLUGINS_REPOSITORY_NAME)
                    getManualPluginFilename(author, name)
                else getPluginFilename(name)

            if (!pluginFolder.exists()) {
                Timber.e("Plugin folder not found")
                return@withContext false
            }

            val repoFolder = File(pluginFolder, repoName)
            if (!repoFolder.exists()) {
                Timber.e("Plugin repository folder not found: $repoName")
                return@withContext false
            }

            val file = File(repoFolder, filename)
            if (!file.exists()) {
                Timber.e("Plugin file not found: $filename")
                return@withContext false
            }
            try {
                val deleted = file.delete()
                Timber.d("Deleted ${file.name}: $deleted")
                return@withContext deleted
            } catch (ex: IOException) {
                Timber.e("Plugin file not deleted: $ex")
                return@withContext false
            }
        }

    suspend fun removePlugin(context: Context, repository: String, plugin: Plugin): Boolean =
        removePlugin(context, repository, plugin.author, plugin.name)

    suspend fun removePlugin(context: Context, plugin: RepositoryListItem.Plugin): Boolean =
        removePlugin(context, plugin.repository, plugin.author, plugin.name)

    private fun getPluginFromJSON(json: String): Plugin? {
        return try {
            pluginAdapter.fromJson(json)
        } catch (ex: Exception) {
            Timber.e("Error reading json string, exception ${ex.message}")
            null
        }
    }

    fun removeInstalledPlugins(context: Context): Int {
        return try {
            var pluginCounter = 0
            var deleteCounter = 0
            val pluginFolder = context.getDir("plugins", Context.MODE_PRIVATE)
            pluginFolder.walk().forEach {
                if (it.isFile) {
                    pluginCounter++
                    if (it.delete()) deleteCounter++
                }
            }
            val deleted = pluginFolder.deleteRecursively()
            Timber.d(
                "Found $pluginCounter plugins, deleted $deleteCounter plugins. Main directory deleted: $deleted"
            )
            deleteCounter
        } catch (e: SecurityException) {
            Timber.d("Security exception deleting plugins files: ${e.message}")
            -1
        }
    }

    fun removeRepositoryPlugins(context: Context, repositoryUrl: String): Int {
        val repoName = getRepositoryString(repositoryUrl)
        return try {
            val pluginFolder = context.getDir("plugins", Context.MODE_PRIVATE)
            val repoFolder = File(pluginFolder, repoName)
            if (repoFolder.exists()) {
                val pluginCounter = repoFolder.listFiles()?.size ?: 0
                val deleted = repoFolder.deleteRecursively()
                if (deleted) return pluginCounter else return 0
            } else Timber.d("No plugin folder for repository $repositoryUrl found")
            0
        } catch (e: SecurityException) {
            Timber.e("Security exception deleting plugins files: ${e.message}")
            -1
        }
    }

    suspend fun addExternalPlugin(pluginsFolder: File, pluginFile: File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val savedPluginPath = File(pluginsFolder, pluginFile.name)
                // remove file if already existing
                if (savedPluginPath.exists()) savedPluginPath.delete()
                // copy plugin from cache to internal memory
                pluginFile.inputStream().use { inputStream ->
                    savedPluginPath.outputStream().use { inputStream.copyTo(it) }
                }
            } catch (exception: Exception) {
                Timber.e("Error saving the plugin ${pluginFile.name}: ${exception.message}")
                return@withContext false
            }
            return@withContext true
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

    suspend fun readPluginFile(pluginFile: File): Plugin? =
        withContext(Dispatchers.IO) {
            try {
                pluginFile.bufferedReader().use {
                    val json = it.readText()
                    return@withContext getPluginFromJSON(json)
                }
            } catch (exception: Exception) {
                Timber.e("Error adding the plugin ${pluginFile.name}: ${exception.message}")
            }
            null
        }

    suspend fun savePlugin(
        context: Context,
        plugin: Plugin,
        repositoryURL: String?
    ): InstallResult =
        withContext(Dispatchers.IO) {
            // we use the repo link hash as folder name for all the plugins from that repo
            val repoName =
                if (repositoryURL != null) getRepositoryString(repositoryURL)
                else MANUAL_PLUGINS_REPOSITORY_NAME

            /*
            issue: we have 3 possible installation modes:
            1. local file shared to the app
            2. file url shared to the app
            3. download from a repository

            while 3 is easy to manage, 1 and 2 means the file will go to a custom folder
            how to manage deletion/update of these files?
            In particular let's say we have 2 plugins with the same name, how to differentiate them?
            */

            val filename =
                if (repositoryURL != null) getPluginFilename(plugin.name)
                else getManualPluginFilename(plugin.author, plugin.name)

            try {
                val pluginFolder = context.getDir("plugins", Context.MODE_PRIVATE)
                if (!pluginFolder.exists()) pluginFolder.mkdirs()

                val repoFolder = File(pluginFolder, repoName)
                if (!repoFolder.exists()) repoFolder.mkdirs()

                val file = File(repoFolder, filename)

                // this will overwrite the file, no deletion needed
                // utf 8 is enough?
                file.writeText(pluginAdapter.toJson(plugin))
            } catch (exception: IOException) {
                Timber.e("Error saving into memory the plugin $filename: ${exception.message}")
                return@withContext InstallResult.Error(exception)
            } catch (exception: Exception) {
                Timber.e("Unknown error adding the plugin $filename: ${exception.message}")
                return@withContext InstallResult.Error(exception)
            }

            return@withContext InstallResult.Installed
        }

    suspend fun savePluginFromDisk(context: Context, data: Uri): InstallResult {
        val plugin: Plugin? = readPassedPlugin(context, data)
        if (plugin != null) {
            if (!plugin.isCompatible()) return InstallResult.Incompatible
            val saved = savePlugin(context, plugin, null)
            return saved
        }
        return InstallResult.Error(IllegalArgumentException("Could not load plugin from $data"))
    }

    companion object {
        const val TYPE_UNCHAINED = ".unchained"
    }
}

sealed class InstallResult {
    object Installed : InstallResult()
    data class Error(val exception: Exception) : InstallResult()
    object Incompatible : InstallResult()
}

data class LocalPlugins(val pluginsData: Map<String, List<Plugin>>, val errors: Int)

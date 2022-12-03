package com.github.livingwithhippos.unchained.search.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.DownloadResult
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.search.model.PluginStatus
import com.github.livingwithhippos.unchained.search.model.PluginVersion
import com.github.livingwithhippos.unchained.search.model.RemotePlugin
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PLUGINS_PACK_FOLDER
import com.github.livingwithhippos.unchained.utilities.UnzipUtils
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val pluginRepository: PluginRepository,
    private val customDownloadRepository: CustomDownloadRepository,
) : ViewModel() {

    val pluginsLiveData = MutableLiveData<Event<PluginEvent>>()

    fun checkRepository() {
        viewModelScope.launch {
            when (val response = customDownloadRepository.downloadPluginRepository()) {
                is EitherResult.Failure -> {
                    pluginsLiveData.postEvent(PluginEvent.RepositoryError(response.failure.toString()))
                }
                is EitherResult.Success -> {
                    pluginsLiveData.postEvent(PluginEvent.Repository(response.success))
                }
            }
        }
    }

    /**
     * Compare the plugins from the repository with the locally installed plugins
     * posts the list of plugins from the online repository with their status populated (new, has update, ready...)
     *
     * @param plugins the list of plugins from the online repository
     */
    fun checkWithLocalPlugins(plugins: List<RemotePlugin>) {
        viewModelScope.launch {
            val checkedPlugins = mutableListOf<RemotePlugin>()
            val localPlugins: List<Plugin> = pluginRepository.getPluginsNew().first
            for (remotePlugin in plugins) {
                var found = false
                val lastRemoteVersion: PluginVersion? = remotePlugin
                    .versions.filter { Parser.isPluginSupported(it.engine) }
                    .maxByOrNull { it.plugin }
                if (lastRemoteVersion == null) {
                    // no supported plugin version for current search engine version
                    // probably user needs to update the app
                    checkedPlugins.add(
                        remotePlugin.apply {
                            status = PluginStatus.uncompatible
                        }
                    )
                } else {
                    for (localPlugin in localPlugins) {
                        if (remotePlugin.id.equals(localPlugin.name, ignoreCase = true)) {
                            found = true
                            if (lastRemoteVersion.engine > localPlugin.engineVersion) {
                                checkedPlugins.add(
                                    remotePlugin.apply {
                                        status = PluginStatus.hasUpdate
                                    }
                                )
                            } else {
                                checkedPlugins.add(
                                    remotePlugin.apply {
                                        status = PluginStatus.ready
                                    }
                                )
                            }
                            break
                        }
                    }
                    // remote plugin not found locally
                    if (!found) {
                        checkedPlugins.add(
                            remotePlugin.apply {
                                status = PluginStatus.isNew
                            }
                        )
                    }
                }

            }
            pluginsLiveData.postEvent(PluginEvent.CheckedPlugins(checkedPlugins))
        }
    }

    fun downloadPlugins(link: String, fileName: String, directory: File, suffix: String) {
        viewModelScope.launch {
            val pluginsFolder = File(directory, PLUGINS_PACK_FOLDER)
            customDownloadRepository.downloadToCache(link, fileName, pluginsFolder, suffix).collect {
                /*
                when(it) {
                    is DownloadResult.End -> {
                        val plugin = pluginRepository.readPluginFile(it.fileName)
                        if (plugin != null) {
                            val installed = pluginRepository.addExternalPlugin(pluginsDir, pluginFile)
                            if (installed) {
                                Timber.d("Installed plugin ${pluginFile.name}")
                                installedPlugins++
                            } else
                                Timber.d("Error installing plugin ${pluginFile.name}")
                        } else {
                            Timber.d("Error parsing plugin ${pluginFile.name}")
                        }
                        pluginsLiveData.postEvent(PluginEvent.Downloaded(it.fileName))
                    }
                    DownloadResult.Failure -> {

                    }
                    is DownloadResult.Progress -> {

                    }
                    DownloadResult.WrongURL -> {

                    }
                }
                 */
            }
        }
    }


    fun processPluginFile(cacheDir: File, fileName: String) {
        /*
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cacheFile = File(cacheDir, fileName)
                    UnzipUtils.unzip(cacheFile, File(cacheDir, PLUGINS_PACK_FOLDER))
                    Timber.d("Zip pack extracted")
                    installPluginsPack(cacheDir, pluginsDir)
                } catch (exception: IOException) {
                    Timber.e("Plugins pack IOException error with the file: ${exception.message}")
                } catch (exception: FileNotFoundException) {
                    Timber.e("Plugins pack: file not found: ${exception.message}")
                } catch (exception: Exception) {
                    Timber.e("Plugins pack: Other error getting the file: ${exception.message}")
                }
            }
        }
         */
    }

    fun downloadAllPlugins(plugins: List<RemotePlugin>) {
        /*
        for (plugin in plugins.filter {
            it.status == PluginStatus.isNew ||
                    it.status == PluginStatus.hasUpdate
        }) {
            val version: PluginVersion = plugin.versions.filter { Parser.isPluginSupported(it.engine) }.maxBy { it.plugin }
            viewModel.downloadPlugins(version.link, plugin.id, requireContext().cacheDir,"unchained")
        }
         */
    }


}

sealed class PluginEvent {
    data class Repository(val plugins: List<RemotePlugin>): PluginEvent()
    data class RepositoryError(val error: String): PluginEvent()
    data class CheckedPlugins(val plugins: List<RemotePlugin>): PluginEvent()
    data class Downloaded(val fileName: String): PluginEvent()
}
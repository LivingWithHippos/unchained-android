package com.github.livingwithhippos.unchained.search.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.search.model.PluginStatus
import com.github.livingwithhippos.unchained.search.model.PluginVersion
import com.github.livingwithhippos.unchained.search.model.RemotePlugin
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
                    .versions.filter { isPluginSupported(it.engine) }
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

    private fun isPluginSupported(engineVersion: Double): Boolean {
        return engineVersion.toInt() == Parser.PLUGIN_ENGINE_VERSION.toInt() && Parser.PLUGIN_ENGINE_VERSION >= engineVersion
    }
}

sealed class PluginEvent {
    data class Repository(val plugins: List<RemotePlugin>): PluginEvent()
    data class RepositoryError(val error: String): PluginEvent()
    data class CheckedPlugins(val plugins: List<RemotePlugin>): PluginEvent()
}
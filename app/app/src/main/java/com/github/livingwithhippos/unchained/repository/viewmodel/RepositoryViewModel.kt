package com.github.livingwithhippos.unchained.repository.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.PluginVersion
import com.github.livingwithhippos.unchained.data.model.RepositoryInfo
import com.github.livingwithhippos.unchained.data.model.RepositoryPlugin
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.DatabasePluginRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.utilities.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class RepositoryViewModel @Inject constructor(
    private val pluginsRepository: DatabasePluginRepository,
    private val downloadRepository: CustomDownloadRepository,
) : ViewModel() {
    val pluginsRepositoryLiveData = MutableLiveData<Event<PluginRepositoryEvent>>()

    private val moshi: Moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<Plugin> = moshi.adapter(Plugin::class.java)


    fun checkCurrentRepositories() {
        viewModelScope.launch {
            val repositories = pluginsRepository.getRepositoriesLink()
            for (repo in repositories) {
                when (val info = downloadRepository.downloadPluginRepository(repo.link)) {
                    is EitherResult.Failure -> {
                        Timber.w("Error downloading repo at ${repo.link}: ${info.failure}")
                    }
                    is EitherResult.Success -> {
                        pluginsRepository.saveRepositoryInfo(
                            repo.link,
                            info.success
                        )
                    }
                }
            }
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.Updated
            )
        }
    }

    fun retrieveDatabaseRepositories() {
        viewModelScope.launch {
            val repo = pluginsRepository.getFullRepositoriesData()
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.FullData(repo)
            )
        }
    }

    fun filterList(query: String?) {
        viewModelScope.launch {
            val repo = pluginsRepository.getFilteredRepositoriesData(query?.trim() ?: "")
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.FullData(repo)
            )
        }
    }

    fun downloadPlugin(link: String, repository: String, context: Context) {
        viewModelScope.launch {
            when (val result = downloadRepository.downloadPlugin(link)) {
                is EitherResult.Failure -> {
                    Timber.e("Error downloading plugin at $link:\n${result.failure}")
                }
                is EitherResult.Success -> {
                    // we use the repo hash as folder name for all the plugins from that repo
                    val repoName = getRepositoryString(repository)
                    val filename = getPluginFilename(result.success.name)
                    try {
                        val pluginFolder = context.getDir("plugins", Context.MODE_PRIVATE)
                        if (!pluginFolder.exists())
                            pluginFolder.mkdirs()

                        val repoFolder = File(pluginFolder, repoName)
                        if (!repoFolder.exists())
                            repoFolder.mkdirs()

                        val file = File(repoFolder, filename)
                        // utf 8 is enough?
                        file.writeText(jsonAdapter.toJson(result.success))

                        pluginsRepositoryLiveData.postEvent(
                            PluginRepositoryEvent.Installed
                        )
                    } catch (exception: IOException) {
                        Timber.e("Error adding the plugin $filename: ${exception.message}")
                    }
                }
            }
        }
    }
}

sealed class PluginRepositoryEvent {
    object Updated: PluginRepositoryEvent()
    data class FullData(val data: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>) : PluginRepositoryEvent()
    object Installed: PluginRepositoryEvent()
}
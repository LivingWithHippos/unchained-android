package com.github.livingwithhippos.unchained.repository.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.PluginVersion
import com.github.livingwithhippos.unchained.data.model.RepositoryInfo
import com.github.livingwithhippos.unchained.data.model.RepositoryPlugin
import com.github.livingwithhippos.unchained.data.repository.*
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RepositoryViewModel @Inject constructor(
    private val databasePluginsRepository: DatabasePluginRepository,
    private val diskPluginsRepository: PluginRepository,
    private val downloadRepository: CustomDownloadRepository,
) : ViewModel() {
    val pluginsRepositoryLiveData = MutableLiveData<Event<PluginRepositoryEvent>>()

    private val moshi: Moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<Plugin> = moshi.adapter(Plugin::class.java)


    fun checkCurrentRepositories() {
        viewModelScope.launch {
            val repositories = databasePluginsRepository.getRepositoriesLink()
            for (repo in repositories) {
                when (val info = downloadRepository.downloadPluginRepository(repo.link)) {
                    is EitherResult.Failure -> {
                        Timber.e("Error downloading repo at ${repo.link}: ${info.failure}")
                    }
                    is EitherResult.Success -> {
                        databasePluginsRepository.saveRepositoryInfo(
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
            val repo = databasePluginsRepository.getFullRepositoriesData()
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.FullData(repo)
            )
        }
    }

    fun filterList(query: String?) {
        viewModelScope.launch {
            val repo = databasePluginsRepository.getFilteredRepositoriesData(query?.trim() ?: "")
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.FullData(repo)
            )
        }
    }

    /**
     * Download a plugin, expects  link that can be read as text.
     * Links are downloaded into the internal app memory. If a repository name is provided
     * they will be put under a directory related to that repo, otherwise they'll go into a
     * common custom_repo folder and the url will be used to get the file name, so that a download
     * to the same link can avoid showing up twice in the download list
     *
     * @param link
     * @param repository: the link to the repository or null if it was downloaded directly
     * @param context
     */
    fun downloadPlugin(link: String, repository: String?, context: Context) {
        viewModelScope.launch {
            when (val result = downloadRepository.downloadPlugin(link)) {
                is EitherResult.Failure -> {
                    Timber.e("Error downloading plugin at $link:\n${result.failure}")
                }
                is EitherResult.Success -> {
                    val saved = diskPluginsRepository.savePlugin(
                        context,
                        result.success,
                        link,
                        repository
                    )

                    when(saved) {
                        is InstallResult.Error -> pluginsRepositoryLiveData.postEvent(
                            PluginRepositoryEvent.NotInstalled
                        )
                        InstallResult.Incompatible -> pluginsRepositoryLiveData.postEvent(
                            PluginRepositoryEvent.NotInstalled
                        )
                        InstallResult.Installed -> pluginsRepositoryLiveData.postEvent(
                            PluginRepositoryEvent.Installed
                        )
                    }

                }
            }
        }
    }
}

sealed class PluginRepositoryEvent {
    object Updated : PluginRepositoryEvent()
    data class FullData(val data: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>) :
        PluginRepositoryEvent()
    object Installed : PluginRepositoryEvent()
    object NotInstalled : PluginRepositoryEvent()
}
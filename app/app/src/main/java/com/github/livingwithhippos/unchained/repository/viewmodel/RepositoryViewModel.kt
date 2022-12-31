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
import com.github.livingwithhippos.unchained.repository.model.RepositoryListItem
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.getRepositoryString
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

    fun retrieveDatabaseRepositories(context: Context) {
        viewModelScope.launch {
            val repo: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>> = databasePluginsRepository.getFullRepositoriesData()
            val localPlugins: LocalPlugins = fetchInstalledPlugins(context)
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.FullData(repo, localPlugins)
            )
        }
    }

    fun filterList(context: Context, query: String?) {
        viewModelScope.launch {
            val repo = databasePluginsRepository.getFilteredRepositoriesData(query?.trim() ?: "")
            val localPlugins: LocalPlugins = fetchInstalledPlugins(context)
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.FullData(repo, localPlugins)
            )
        }
    }

    fun uninstallPlugin(context: Context, plugin: RepositoryListItem.Plugin) {
        viewModelScope.launch {
            val result = diskPluginsRepository.removePlugin(context, plugin.repository, plugin.name)
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.Uninstalled(if (result) 1 else 0)
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
     * @param repositoryURL: the link to the repository or null if it was downloaded directly
     * @param context
     */
    fun downloadPlugin(link: String, repositoryURL: String?, context: Context) {
        viewModelScope.launch {
            when (val result = downloadRepository.downloadPlugin(link)) {
                is EitherResult.Failure -> {
                    Timber.e("Error downloading plugin at $link:\n${result.failure}")
                }
                is EitherResult.Success -> {
                    val install = diskPluginsRepository.savePlugin(
                        context,
                        result.success,
                        link,
                        repositoryURL
                    )
                    pluginsRepositoryLiveData.postEvent(
                        PluginRepositoryEvent.Installation(install)
                    )
                }
            }
        }
    }

    private suspend fun fetchInstalledPlugins(context: Context) = diskPluginsRepository.getPluginsWithFolders(context)

    fun installAllRepositoryPlugins(context: Context, repository: RepositoryListItem.Repository) {
        viewModelScope.launch {
            // get plugins from db
            // filter by repo and compatibility
            val plugins = databasePluginsRepository.getLatestCompatibleRepositoryPlugins(repository.link)
            installMultiplePlugins(context, plugins)
        }

    }

    private suspend fun installMultiplePlugins(context: Context, plugins: List<PluginVersion>) {
        var errors = 0
        val installResults = mutableListOf<InstallResult>()
        // install all
        for (plugin in plugins) {
            when (val result = downloadRepository.downloadPlugin(plugin.link)) {
                is EitherResult.Failure -> {
                    Timber.e("Error downloading plugin at ${plugin.link}:\n${result.failure}")
                    errors++
                }
                is EitherResult.Success -> {
                    val install = diskPluginsRepository.savePlugin(
                        context,
                        result.success,
                        plugin.link,
                        plugin.repository
                    )
                    installResults.add(install)
                }
            }
        }

        // report errors and installed
        pluginsRepositoryLiveData.postEvent(
            PluginRepositoryEvent.MultipleInstallation(errors, installResults)
        )
    }

    fun updateAllRepositoryPlugins(context: Context, repository: RepositoryListItem.Repository) {
        viewModelScope.launch {
            val repoName = getRepositoryString(repository.link)
            val remotePlugins: List<PluginVersion> = databasePluginsRepository.getLatestCompatibleRepositoryPlugins(repository.link)
            val installedPlugins: List<Plugin>? = fetchInstalledPlugins(context).pluginsData[repoName]
            if (installedPlugins.isNullOrEmpty()) {
                installMultiplePlugins(context, emptyList())
            } else {
               val updatablePlugins = remotePlugins.filter { remotePlugin ->
                   val installedVersion: Plugin? = installedPlugins.firstOrNull{
                        it.name == remotePlugin.plugin
                    }

                   if (installedVersion == null)
                       false
                   else
                       installedVersion.version < remotePlugin.version
                }

                installMultiplePlugins(context, updatablePlugins)
            }
        }
    }
}

sealed class PluginRepositoryEvent {

    data class Installation(val result: InstallResult): PluginRepositoryEvent()

    data class MultipleInstallation(val downloadErrors: Int, val installResults: List<InstallResult>): PluginRepositoryEvent()
    data class Uninstalled(val quantity: Int): PluginRepositoryEvent()
    object Updated : PluginRepositoryEvent()
    data class FullData(
        val dbData: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>,
        val installedData: LocalPlugins
        ) : PluginRepositoryEvent()
}
package com.github.livingwithhippos.unchained.repository.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.PluginVersion
import com.github.livingwithhippos.unchained.data.model.RepositoryInfo
import com.github.livingwithhippos.unchained.data.model.RepositoryPlugin
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.DatabasePluginRepository
import com.github.livingwithhippos.unchained.data.repository.InstallResult
import com.github.livingwithhippos.unchained.data.repository.LocalPlugins
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.repository.model.JsonPluginRepository
import com.github.livingwithhippos.unchained.repository.model.RepositoryListItem
import com.github.livingwithhippos.unchained.utilities.DEFAULT_PLUGINS_REPOSITORY_LINK
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import com.github.livingwithhippos.unchained.utilities.getRepositoryString
import com.github.livingwithhippos.unchained.utilities.postEvent
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import timber.log.Timber

@HiltViewModel
class RepositoryViewModel
@Inject
constructor(
    private val databasePluginsRepository: DatabasePluginRepository,
    private val diskPluginsRepository: PluginRepository,
    private val downloadRepository: CustomDownloadRepository,
) : ViewModel() {
    val pluginsRepositoryLiveData = MutableLiveData<Event<PluginRepositoryEvent>>()

    // todo: inject
    private val jsonAdapter: JsonAdapter<JsonPluginRepository> =
        Moshi.Builder().build().adapter(JsonPluginRepository::class.java)

    fun checkCurrentRepositories() {
        viewModelScope.launch {
            val repositories = databasePluginsRepository.getRepositoriesLink()
            for (repo in repositories) {
                when (val info = downloadRepository.downloadPluginRepository(repo.link)) {
                    is EitherResult.Failure -> {
                        Timber.e("Error downloading repo at ${repo.link}: ${info.failure}")
                    }
                    is EitherResult.Success -> {
                        databasePluginsRepository.saveRepositoryInfo(repo.link, info.success)
                    }
                }
            }

            pluginsRepositoryLiveData.postEvent(PluginRepositoryEvent.Updated)
        }
    }

    fun retrieveDatabaseRepositories(context: Context) {
        viewModelScope.launch {
            val repo: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>> =
                databasePluginsRepository.getFullRepositoriesData()
            val localPlugins: LocalPlugins = fetchInstalledPlugins(context)
            pluginsRepositoryLiveData.postEvent(PluginRepositoryEvent.FullData(repo, localPlugins))
        }
    }

    fun filterList(context: Context, query: String?) {
        viewModelScope.launch {
            val repo = databasePluginsRepository.getFilteredRepositoriesData(query?.trim() ?: "")
            val localPlugins: LocalPlugins = fetchInstalledPlugins(context)
            pluginsRepositoryLiveData.postEvent(PluginRepositoryEvent.FullData(repo, localPlugins))
        }
    }

    fun uninstallPlugin(context: Context, plugin: RepositoryListItem.Plugin) {
        viewModelScope.launch {
            val result = diskPluginsRepository.removePlugin(context, plugin)
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.Uninstalled(if (result) 1 else 0)
            )
        }
    }

    /**
     * Download a plugin, expects link that can be read as text. Links are downloaded into the
     * internal app memory. If a repository name is provided they will be put under a directory
     * related to that repo, otherwise they'll go into a common custom_repo folder and the url will
     * be used to get the file name, so that a download to the same link can avoid showing up twice
     * in the download list
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
                    val install =
                        diskPluginsRepository.savePlugin(context, result.success, repositoryURL)
                    pluginsRepositoryLiveData.postEvent(PluginRepositoryEvent.Installation(install))
                }
            }
        }
    }

    private suspend fun fetchInstalledPlugins(context: Context) =
        diskPluginsRepository.getPluginsWithFolders(context)

    fun installAllRepositoryPlugins(context: Context, repository: RepositoryListItem.Repository) {
        viewModelScope.launch {
            // get plugins from db
            // filter by repo and compatibility
            val plugins =
                databasePluginsRepository.getLatestCompatibleRepositoryPlugins(repository.link)
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
                    val install =
                        diskPluginsRepository.savePlugin(context, result.success, plugin.repository)
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
            val remotePlugins: List<PluginVersion> =
                databasePluginsRepository.getLatestCompatibleRepositoryPlugins(repository.link)
            val installedPlugins: List<Plugin>? =
                fetchInstalledPlugins(context).pluginsData[repoName]
            if (installedPlugins.isNullOrEmpty()) {
                installMultiplePlugins(context, emptyList())
            } else {
                val updatablePlugins =
                    remotePlugins.filter { remotePlugin ->
                        val installedVersion: Plugin? =
                            installedPlugins.firstOrNull { it.name == remotePlugin.plugin }

                        if (installedVersion == null) false
                        else installedVersion.version < remotePlugin.version
                    }

                installMultiplePlugins(context, updatablePlugins)
            }
        }
    }

    fun uninstallAllRepositoryPlugins(context: Context, repository: RepositoryListItem.Repository) {
        viewModelScope.launch {
            val result = diskPluginsRepository.removeRepositoryPlugins(context, repository.link)
            pluginsRepositoryLiveData.postEvent(PluginRepositoryEvent.Uninstalled(result))
        }
    }

    fun uninstallRepository(context: Context, repository: RepositoryListItem.Repository) {
        viewModelScope.launch {
            val pluginResult: Int =
                diskPluginsRepository.removeRepositoryPlugins(context, repository.link)
            // avoid removing from the db the default repository
            if (repository.link != DEFAULT_PLUGINS_REPOSITORY_LINK)
                databasePluginsRepository.removeRepository(repository.link)
            // use another event?
            pluginsRepositoryLiveData.postEvent(PluginRepositoryEvent.Uninstalled(pluginResult))
        }
    }

    private suspend fun getRepositoryResult(url: String): PluginRepositoryEvent {
        if (url.trim().isWebUrl().not()) {
            return (PluginRepositoryEvent.InvalidRepositoryLink(InvalidLinkReason.NotAnUrl))
        } else {
            when (val result = downloadRepository.downloadAsString(url.trim())) {
                is EitherResult.Failure -> {
                    Timber.e("Error downloading repo at ${url}: ${result.failure}")
                    return (PluginRepositoryEvent.InvalidRepositoryLink(
                        InvalidLinkReason.ConnectionError
                    ))
                }
                is EitherResult.Success -> {
                    return try {
                        val repository = jsonAdapter.fromJson(result.success)
                        if (repository != null)
                            (PluginRepositoryEvent.ValidRepositoryLink(repository))
                        else
                            (PluginRepositoryEvent.InvalidRepositoryLink(
                                InvalidLinkReason.ParsingError
                            ))
                    } catch (ex: JSONException) {
                        Timber.e("Error while parsing repo from $url:\n${ex.message}")
                        (PluginRepositoryEvent.InvalidRepositoryLink(
                            InvalidLinkReason.ParsingError
                        ))
                    } catch (ex: JsonEncodingException) {
                        Timber.e("Error while parsing repo from $url:\n${ex.message}")
                        (PluginRepositoryEvent.InvalidRepositoryLink(
                            InvalidLinkReason.ParsingError
                        ))
                    }
                }
            }
        }
    }

    fun checkRepositoryLink(url: String) {
        viewModelScope.launch {
            val result = getRepositoryResult(url)
            pluginsRepositoryLiveData.postEvent(result)
        }
    }

    fun addRepository(url: String) {
        viewModelScope.launch {
            databasePluginsRepository.addRepositoryUrl(url)
            delay(100)
            // used to update on the main screen the repo
            checkCurrentRepositories()
        }
    }
}

sealed class PluginRepositoryEvent {

    data class Installation(val result: InstallResult) : PluginRepositoryEvent()

    data class MultipleInstallation(
        val downloadErrors: Int,
        val installResults: List<InstallResult>
    ) : PluginRepositoryEvent()

    data class Uninstalled(val quantity: Int) : PluginRepositoryEvent()
    object Updated : PluginRepositoryEvent()
    data class FullData(
        val dbData: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>,
        val installedData: LocalPlugins
    ) : PluginRepositoryEvent()

    data class InvalidRepositoryLink(val reason: InvalidLinkReason) : PluginRepositoryEvent()
    data class ValidRepositoryLink(val repository: JsonPluginRepository) : PluginRepositoryEvent()
}

sealed class InvalidLinkReason {
    object NotAnUrl : InvalidLinkReason()
    object ConnectionError : InvalidLinkReason()
    object ParsingError : InvalidLinkReason()
}

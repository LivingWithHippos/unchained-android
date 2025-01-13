package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.RepositoryDataDao
import com.github.livingwithhippos.unchained.data.model.PluginVersion
import com.github.livingwithhippos.unchained.data.model.Repository
import com.github.livingwithhippos.unchained.data.model.RepositoryInfo
import com.github.livingwithhippos.unchained.data.model.RepositoryPlugin
import com.github.livingwithhippos.unchained.plugins.model.isCompatible
import com.github.livingwithhippos.unchained.repository.model.JsonPluginRepository

class DatabasePluginRepository(private val repositoryDataDao: RepositoryDataDao) {
    suspend fun getFullRepositoriesData():
        Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>> {
        val pluginsByRepo: Map<RepositoryInfo, List<RepositoryPlugin>> =
            repositoryDataDao.getPlugins()
        val versionByPlugin: Map<RepositoryPlugin, List<PluginVersion>> =
            repositoryDataDao.getPluginsVersions()
        val pluginsMap = mutableMapOf<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>()
        for (entry in pluginsByRepo) {
            // val repo = entry.key
            val repoPlugins = mutableMapOf<RepositoryPlugin, List<PluginVersion>>()
            for (plugin in entry.value) {
                val pluginVersions: List<PluginVersion>? = versionByPlugin[plugin]
                if (pluginVersions != null) repoPlugins[plugin] = pluginVersions
            }
            pluginsMap[entry.key] = repoPlugins
        }
        return pluginsMap
    }

    suspend fun getLatestCompatibleRepositoryPlugins(link: String): List<PluginVersion> {
        val repository = repositoryDataDao.getRepository(link)
        if (repository == null) {
            return emptyList()
        } else {
            val latestVersions: List<PluginVersion> =
                repositoryDataDao
                    .getRepositoryPluginsVersions(repository.link)
                    .filter { isCompatible(it.engine) }
                    .groupBy { it.plugin }
                    .mapNotNull { entry ->
                        val maxVersion = entry.value.maxOfOrNull { it.version }
                        entry.value.firstOrNull { it.version == maxVersion }
                    }
            return latestVersions
        }
    }

    suspend fun saveRepositoryInfo(link: String, jsonRepository: JsonPluginRepository) {
        repositoryDataDao.insert(
            RepositoryInfo(
                link = link,
                version = jsonRepository.repositoryVersion,
                name = jsonRepository.name,
                description = jsonRepository.description,
                author = jsonRepository.author,
            )
        )
        for (plugin in jsonRepository.plugins) {
            repositoryDataDao.insert(RepositoryPlugin(repository = link, name = plugin.id))
            repositoryDataDao.insert(
                plugin.versions.map {
                    PluginVersion(
                        repository = link,
                        plugin = plugin.id,
                        version = it.plugin,
                        engine = it.engine,
                        link = it.link,
                    )
                }
            )
        }
    }

    suspend fun removeRepository(link: String) {
        repositoryDataDao.delete(Repository(link))
    }

    suspend fun getRepositoriesLink(): List<Repository> {
        return repositoryDataDao.getAllRepositories()
    }

    suspend fun addRepositoryUrl(url: String) {
        repositoryDataDao.insert(Repository(url))
    }

    /**
     * Get all the plugins or repository filtered by the query. If a plugin is a match the
     * repository will also be returned (useful for separating results from different repositories)
     *
     * @param query
     * @return
     */
    suspend fun getFilteredRepositoriesData(
        query: String
    ): Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>> {
        val pluginsByRepo: Map<RepositoryInfo, List<RepositoryPlugin>> =
            repositoryDataDao.getPlugins("%$query%")
        val versionByPlugin: Map<RepositoryPlugin, List<PluginVersion>> =
            repositoryDataDao.getPluginsVersions()
        val pluginsMap = mutableMapOf<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>()
        for (entry in pluginsByRepo) {
            // val repo = entry.key
            val repoPlugins = mutableMapOf<RepositoryPlugin, List<PluginVersion>>()
            for (plugin in entry.value) {
                val pluginVersions: List<PluginVersion>? = versionByPlugin[plugin]
                if (pluginVersions != null) repoPlugins[plugin] = pluginVersions
            }
            pluginsMap[entry.key] = repoPlugins
        }
        return pluginsMap
    }

    suspend fun getEnabledPlugins(): Map<RepositoryInfo, List<RepositoryPlugin>> =
        repositoryDataDao.getEnabledPlugins()

    suspend fun enablePlugin(name: String, enabled: Boolean) {
        repositoryDataDao.enablePlugin(name, enabled)
    }
}

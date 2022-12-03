package com.github.livingwithhippos.unchained.data.local

import androidx.room.*
import com.github.livingwithhippos.unchained.data.model.*
import com.github.livingwithhippos.unchained.utilities.PLUGINS_REPOSITORY_LINK

@Dao
interface RepositoryDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repository: Repository)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plugin: RepositoryPlugin)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repository: RepositoryInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(versions: List<PluginVersion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: PluginVersion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInfo(list: List<RepositoryInfo>)

    @Query("SELECT * from repository_info")
    suspend fun getAllRepositoriesInfo(): List<RepositoryInfo>

    @Query("SELECT * from repository")
    suspend fun getAllRepositories(): List<Repository>

    @Query("SELECT * from repository WHERE repository.link =:link")
    suspend fun getDefaultRepository(link: String = PLUGINS_REPOSITORY_LINK): List<Repository>

    @Query("DELETE FROM repository")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(repository: Repository)

    @Query(
        "SELECT * FROM repository_info JOIN " +
                "plugin ON plugin.repository = repository_info.link"
    )
    suspend fun getPlugins(): Map<RepositoryInfo, List<RepositoryPlugin>>

    @Query(
        "SELECT * FROM plugin JOIN plugin_version " +
                "ON plugin.repository = plugin_version.plugin_repository " +
                "AND plugin.plugin_name = plugin_version.plugin"
    )
    suspend fun getPluginsVersions(): Map<RepositoryPlugin, List<PluginVersion>>

    /*
    @Query(
        "SELECT * FROM repository_info " +
                "LEFT JOIN plugin " +
                "ON plugin.repository = repository_info.link " +
                "LEFT JOIN plugin_version " +
                "ON plugin.repository = plugin_version.repository " +
                "AND plugin.name = plugin_version.plugin"
    )
    suspend fun getAll(): Map<RepositoryInfo, List<Map<RepositoryPlugin, List<PluginVersion>>>>
     */
}

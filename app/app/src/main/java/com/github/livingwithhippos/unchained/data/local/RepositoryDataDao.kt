package com.github.livingwithhippos.unchained.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.livingwithhippos.unchained.data.model.PluginVersion
import com.github.livingwithhippos.unchained.data.model.Repository
import com.github.livingwithhippos.unchained.data.model.RepositoryInfo
import com.github.livingwithhippos.unchained.data.model.RepositoryPlugin
import com.github.livingwithhippos.unchained.utilities.DEFAULT_PLUGINS_REPOSITORY_LINK

@Dao
interface RepositoryDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(repository: Repository)

    // since the repository plugin has only the search enabled column that can be changed and it's a
    // user setting we can safely skip inserting on conflicts
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(plugin: RepositoryPlugin)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(repository: RepositoryInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(versions: List<PluginVersion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(version: PluginVersion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInfo(list: List<RepositoryInfo>)

    @Query("SELECT * from repository_info")
    suspend fun getAllRepositoriesInfo(): List<RepositoryInfo>

    @Query("SELECT * from repository") suspend fun getAllRepositories(): List<Repository>

    @Query("SELECT * from repository WHERE repository.link = :link LIMIT 1")
    suspend fun getRepository(link: String): Repository?

    @Query("SELECT * from repository WHERE repository.link =:link")
    suspend fun getDefaultRepository(
        link: String = DEFAULT_PLUGINS_REPOSITORY_LINK
    ): List<Repository>

    @Query("DELETE FROM repository") suspend fun deleteAll()

    @Delete suspend fun delete(repository: Repository)

    @Query("SELECT * FROM repository_info JOIN plugin ON plugin.repository = repository_info.link")
    suspend fun getPlugins(): Map<RepositoryInfo, List<RepositoryPlugin>>

    @Query(
        "SELECT * FROM repository_info JOIN plugin ON plugin.repository = repository_info.link WHERE plugin.search_enabled = 1"
    )
    suspend fun getEnabledPlugins(): Map<RepositoryInfo, List<RepositoryPlugin>>

    @Query(
        "SELECT * FROM repository_info JOIN " +
            "plugin ON plugin.repository = repository_info.link " +
            "WHERE plugin.plugin_name LIKE :query OR repository_info.name LIKE :query"
    )
    suspend fun getPlugins(query: String): Map<RepositoryInfo, List<RepositoryPlugin>>

    @Query(
        "SELECT * FROM repository_info JOIN plugin " +
            "ON plugin.repository = repository_info.link " +
            "WHERE repository_info.link = :repositoryUrl"
    )
    suspend fun getRepositoryPlugins(
        repositoryUrl: String
    ): Map<RepositoryInfo, List<RepositoryPlugin>>

    @Query(
        "SELECT * FROM plugin JOIN plugin_version " +
            "ON plugin.repository = plugin_version.plugin_repository " +
            "AND plugin.plugin_name = plugin_version.plugin"
    )
    suspend fun getPluginsVersions(): Map<RepositoryPlugin, List<PluginVersion>>

    @Query(
        "SELECT * FROM plugin JOIN plugin_version " +
            "ON plugin.repository = plugin_version.plugin_repository " +
            "AND plugin.plugin_name = plugin_version.plugin " +
            "WHERE plugin.repository = :repositoryUrl"
    )
    suspend fun getRepositoryPluginsData(
        repositoryUrl: String
    ): Map<RepositoryPlugin, List<PluginVersion>>

    @Query(
        "SELECT * FROM  plugin_version " +
            "WHERE plugin_version.plugin_repository = :repositoryUrl "
    )
    suspend fun getRepositoryPluginsVersions(repositoryUrl: String): List<PluginVersion>

    @Query("UPDATE plugin SET search_enabled = :enabled WHERE plugin_name = :name")
    suspend fun enablePlugin(name: String, enabled: Boolean)

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

data class LatestPluginVersion(
    @ColumnInfo(name = "plugin_link") val link: String,
    @ColumnInfo(name = "version") val version: Float,
)

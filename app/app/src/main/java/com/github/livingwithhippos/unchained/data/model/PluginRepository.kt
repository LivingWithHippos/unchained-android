package com.github.livingwithhippos.unchained.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey

@Entity(tableName = "repository")
data class Repository(@PrimaryKey @ColumnInfo(name = "link") val link: String)

@Entity(
    tableName = "repository_info",
    foreignKeys =
        [
            ForeignKey(
                entity = Repository::class,
                parentColumns = ["link"],
                childColumns = ["link"],
                onDelete = CASCADE)],
)
data class RepositoryInfo(
    @PrimaryKey @ColumnInfo(name = "link") val link: String,
    @ColumnInfo(name = "version") val version: Double,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "author") val author: String
)

@Entity(
    tableName = "plugin",
    foreignKeys =
        [
            ForeignKey(
                entity = RepositoryInfo::class,
                parentColumns = ["link"],
                childColumns = ["repository"],
                onDelete = CASCADE)],
    primaryKeys = ["repository", "plugin_name"])
data class RepositoryPlugin(
    @ColumnInfo(name = "repository") val repository: String,
    @ColumnInfo(name = "plugin_name") val name: String,
    @ColumnInfo(name = "search_enabled") val searchEnabled: Boolean? = null
)

@Entity(
    tableName = "plugin_version",
    foreignKeys =
        [
            ForeignKey(
                entity = RepositoryPlugin::class,
                parentColumns = ["repository", "plugin_name"],
                childColumns = ["plugin_repository", "plugin"],
                onDelete = CASCADE)],
    primaryKeys = ["plugin_repository", "plugin", "version"])
data class PluginVersion(
    @ColumnInfo(name = "plugin_repository") val repository: String,
    @ColumnInfo(name = "plugin") val plugin: String,
    @ColumnInfo(name = "version") val version: Float,
    @ColumnInfo(name = "engine") val engine: Double,
    @ColumnInfo(name = "plugin_link") val link: String
)

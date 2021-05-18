package com.github.livingwithhippos.unchained.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.livingwithhippos.unchained.data.model.HostRegex
import com.github.livingwithhippos.unchained.data.model.REGEX_TYPE_FOLDER
import com.github.livingwithhippos.unchained.data.model.REGEX_TYPE_HOST

/**
 * This Dao manage the queries for the host regexps.
 */
@Dao
interface HostRegexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hostRegex: HostRegex)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<HostRegex>)

    @Query("SELECT * from host_regex")
    suspend fun getAllRegexps(): List<HostRegex>

    @Query("SELECT * from host_regex WHERE host_regex.type = $REGEX_TYPE_HOST")
    suspend fun getHostRegexps(): List<HostRegex>

    @Query("SELECT * from host_regex WHERE host_regex.type = $REGEX_TYPE_FOLDER")
    suspend fun getFoldersRegexps(): List<HostRegex>

    @Query("DELETE FROM host_regex")
    suspend fun deleteAll()

    @Query("DELETE FROM host_regex WHERE host_regex.type = $REGEX_TYPE_HOST")
    fun deleteAllHosts()

    @Query("DELETE FROM host_regex WHERE host_regex.type = $REGEX_TYPE_FOLDER")
    fun deleteAllFolders()
}
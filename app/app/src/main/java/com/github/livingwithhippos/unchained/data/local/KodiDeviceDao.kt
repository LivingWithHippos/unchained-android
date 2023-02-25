package com.github.livingwithhippos.unchained.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface KodiDeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(device: KodiDevice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<KodiDevice>): List<Long>

    @Query("SELECT * from kodi_device") suspend fun getAllDevices(): List<KodiDevice>

    @Query("SELECT * from kodi_device ORDER BY kodi_device.is_default DESC")
    fun getAllDevicesFlow(): Flow<List<KodiDevice>>

    @Query("DELETE FROM kodi_device") suspend fun deleteAll()

    @Query("DELETE FROM kodi_device WHERE name = :name") suspend fun remove(name: String)

    @Query("SELECT * from kodi_device WHERE kodi_device.is_default = 1 LIMIT 1")
    suspend fun getDefault(): KodiDevice?

    @Query("UPDATE kodi_device SET is_default = 1 WHERE name = :name")
    suspend fun setDefault(name: String)

    @Query("UPDATE kodi_device SET is_default = 0 WHERE is_default = 1") suspend fun resetDefaults()

    @Query("UPDATE kodi_device SET is_default = 0 WHERE is_default = 1 AND name != :name")
    suspend fun resetDefaultsExcept(name: String)

    @Query(
        "UPDATE kodi_device SET name = :name, ip = :address, port =:port, username =:username, password=:password, is_default = :isDefault WHERE name = :oldDeviceName"
    )
    suspend fun update(
        name: String,
        address: String,
        port: Int,
        username: String?,
        password: String?,
        isDefault: Int,
        oldDeviceName: String
    )
}

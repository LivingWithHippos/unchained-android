package com.github.livingwithhippos.unchained.data.local

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "remote_device")
class RemoteDevice(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
) : Parcelable

@Dao
interface RemoteDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: RemoteDevice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<RemoteDevice>): List<Long>

    @Query("SELECT * from remote_device")
    suspend fun getAllDevices(): List<RemoteDevice>

    @Query("DELETE FROM remote_device") suspend fun deleteAll()

    @Query("DELETE FROM remote_device WHERE id = :id") suspend fun remove(id: Int)

    @Query("SELECT * from remote_device WHERE remote_device.is_default = 1 LIMIT 1")
    suspend fun getDefault(): KodiDevice?

    @Query("UPDATE remote_device SET is_default = 1 WHERE id = :id")
    suspend fun setDefault(id: Int)

    @Query("UPDATE remote_device SET is_default = 0 WHERE is_default = 1") suspend fun resetDefaults()
}
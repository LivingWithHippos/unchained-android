package com.github.livingwithhippos.unchained.data.local

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import java.util.Objects
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "remote_device")
class RemoteDevice(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other is RemoteDevice) {
            return other.id == id
        }
        return false
    }

    override fun hashCode(): Int = Objects.hash(id)
}

@Dao
interface RemoteDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: RemoteDevice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: RemoteService): Long

    @Delete suspend fun deleteService(service: RemoteService)

    @Delete suspend fun deleteDevice(device: RemoteDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDevices(list: List<RemoteDevice>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllServices(list: List<RemoteService>): List<Long>

    @Query(
        "SELECT * FROM remote_device JOIN remote_service ON remote_device.id = remote_service.device_id")
    suspend fun getDevicesAndServices(): Map<RemoteDevice, List<RemoteService>>

    @Query(
        "SELECT * FROM remote_device JOIN remote_service ON remote_device.id = remote_service.device_id WHERE remote_service.type IN (:types)")
    suspend fun getMediaPlayerDevicesAndServices(
        types: List<Int>
    ): Map<RemoteDevice, List<RemoteService>>

    @Query(
        "SELECT * FROM remote_device JOIN remote_service ON remote_device.id = remote_service.device_id WHERE remote_service.type IN (:types)")
    fun getMediaPlayerDevicesAndServicesFlow(
        types: List<Int>
    ): Flow<Map<RemoteDevice, List<RemoteService>>>

    @Query("SELECT id FROM remote_device WHERE rowid = :rowId")
    suspend fun getDeviceIDByRow(rowId: Long): Int?

    @Query("SELECT id FROM remote_service WHERE rowid = :rowId")
    suspend fun getServiceIDByRow(rowId: Long): Int?

    @Query("SELECT * FROM remote_service WHERE id = :serviceID")
    suspend fun getService(serviceID: Int): RemoteService?

    @Query("SELECT * FROM remote_device WHERE id = :deviceID")
    suspend fun getDevice(deviceID: Int): RemoteDevice?

    @Query("SELECT * FROM remote_service WHERE remote_service.device_id = :deviceId")
    suspend fun getDeviceServices(deviceId: Int): List<RemoteService>

    @Query("SELECT * from remote_device") suspend fun getAllDevices(): List<RemoteDevice>

    @Query("SELECT * from remote_service") suspend fun getAllServices(): List<RemoteService>

    /** Delete has cascade so this will also delete the services */
    @Query("DELETE FROM remote_device") suspend fun deleteAll()

    /** Delete has cascade so this will also delete the services */
    @Query("DELETE FROM remote_device WHERE id = :id") suspend fun removeDevice(id: Int)

    @Query("DELETE FROM remote_service WHERE id = :id") suspend fun removeService(id: Int)

    @Query("DELETE FROM remote_service WHERE device_id = :deviceId")
    suspend fun removeDeviceServices(deviceId: Int)

    @Query("SELECT * from remote_device WHERE remote_device.is_default = 1 LIMIT 1")
    suspend fun getDefaultDevice(): RemoteDevice?

    @Query(
        "SELECT * FROM remote_device JOIN remote_service ON remote_device.id = remote_service.device_id WHERE remote_device.is_default = 1 LIMIT 1")
    suspend fun getDefaultDeviceWithServices(): Map<RemoteDevice, List<RemoteService>>

    @Query("UPDATE remote_device SET is_default = CASE WHEN id = :deviceId THEN 1  ELSE 0 END;")
    suspend fun setDefaultDevice(deviceId: Int)

    @Query(
        "UPDATE remote_service SET is_default = CASE WHEN id = :serviceId THEN 1  ELSE 0 END WHERE device_id = :deviceId;")
    suspend fun setDefaultDeviceService(deviceId: Int, serviceId: Int)
}

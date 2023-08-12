package com.github.livingwithhippos.unchained.data.local

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "remote_service",
    foreignKeys =
        [
            ForeignKey(
                entity = RemoteDevice::class,
                parentColumns = ["id"],
                childColumns = ["device_id"],
                onDelete = ForeignKey.CASCADE
            )
        ],
)
class RemoteService(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "device_id") val device: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "port") val port: Int,
    @ColumnInfo(name = "username") val username: String? = null,
    @ColumnInfo(name = "password") val password: String? = null,
    // service type, see [RemoteServiceType]
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    // extra fields for future needs. Otherwise a new entity linked to this one should work
    // or 1 entity per service type with customized fields
    // or also storing a json object as string and parsing it back
    // extra field example: api token
    @ColumnInfo(name = "field_1") val fieldOne: String = "",
    @ColumnInfo(name = "field_2") val fieldTwo: String = "",
    @ColumnInfo(name = "field_3") val fieldThree: String = ""
) : Parcelable

enum class RemoteServiceType(val value: Int) {
    KODI(0),
    VLC(1),
    JACKETT(2)
}

package com.github.livingwithhippos.unchained.data.local

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.github.livingwithhippos.unchained.R
import java.util.Objects
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
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other is RemoteService) {
            return other.id == id
        }
        return false
    }

    override fun hashCode(): Int = Objects.hash(id)
}

sealed class RemoteServiceType(
    val value: Int,
    val isMediaPlayer: Boolean,
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int
) {
    data object KODI : RemoteServiceType(0, true, R.string.kodi, R.drawable.icon_kodi)

    data object VLC : RemoteServiceType(1, true, R.string.player_vlc, R.drawable.icon_vlc)

    data object JACKETT : RemoteServiceType(2, false, R.string.jackett, R.drawable.icon_jackett)
}

val serviceTypeMap =
    mapOf(
        RemoteServiceType.KODI.value to RemoteServiceType.KODI,
        RemoteServiceType.VLC.value to RemoteServiceType.VLC,
        RemoteServiceType.JACKETT.value to RemoteServiceType.JACKETT
    )

/** Helper class to have all the service details together */
data class RemoteServiceDetails(
    val service: RemoteService,
    val device: RemoteDevice,
    val type: RemoteServiceType
)

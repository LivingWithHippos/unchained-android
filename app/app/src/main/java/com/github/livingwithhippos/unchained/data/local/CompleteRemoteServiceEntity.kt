package com.github.livingwithhippos.unchained.data.local

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Objects
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(
    tableName = "complete_remote_service"
)
class CompleteRemoteService(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "username") val username: String? = null,
    @ColumnInfo(name = "password") val password: String? = null,
    // service type, see [RemoteServiceType]
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    // extra fields for future needs. Otherwise a new entity linked to this one should work
    // or 1 entity per service type with customized fields
    // or also storing a json object as string and parsing it back
    // extra field example: api token
    @ColumnInfo(name = "api_token") val apiToken: String = "",
    @ColumnInfo(name = "field_1") val fieldOne: String = "",
    @ColumnInfo(name = "field_2") val fieldTwo: String = "",
    @ColumnInfo(name = "field_3") val fieldThree: String = "",
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other is CompleteRemoteService) {
            return other.id == id
        }
        return false
    }

    override fun hashCode(): Int = Objects.hash(id)
}


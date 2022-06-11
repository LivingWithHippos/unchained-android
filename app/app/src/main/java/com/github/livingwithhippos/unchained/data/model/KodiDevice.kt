package com.github.livingwithhippos.unchained.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kodi_device")
class KodiDevice(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "ip")
    val address: String,
    @ColumnInfo(name = "port")
    val port: Int,
    @ColumnInfo(name = "username")
    val username: String?,
    @ColumnInfo(name = "password")
    val password: String?,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
)

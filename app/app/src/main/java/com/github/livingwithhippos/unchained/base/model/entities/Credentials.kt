package com.github.livingwithhippos.unchained.base.model.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credentials")
class Credentials(
    @PrimaryKey
    @ColumnInfo(name = "device_code")
    val deviceCode: String,
    @ColumnInfo(name = "client_id")
    val clientId: String?,
    @ColumnInfo(name = "client_secret")
    val clientSecret: String?,
    @ColumnInfo(name = "access_token")
    val accessToken: String?,
    @ColumnInfo(name = "refresh_token")
    val refreshToken: String?
)
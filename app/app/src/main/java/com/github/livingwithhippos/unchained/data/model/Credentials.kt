package com.github.livingwithhippos.unchained.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The credentials db entity.
 * It can store either a token obtained with the oauth process or a private api token which will populate all fields but accessToken with PRIVATE_TOKEN
 */
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

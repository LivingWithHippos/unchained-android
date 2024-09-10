package com.github.livingwithhippos.unchained.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Keep
@JsonClass(generateAdapter = true)
@Parcelize
data class User(
    @Json(name = "id") val id: Int,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "points") val points: Int,
    @Json(name = "locale") val locale: String,
    @Json(name = "avatar") val avatar: String,
    @Json(name = "type") val type: String,
    @Json(name = "premium") val premium: Int,
    @Json(name = "expiration") val expiration: String,
) : Parcelable

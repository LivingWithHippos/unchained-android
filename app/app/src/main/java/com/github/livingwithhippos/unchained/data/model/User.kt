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
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "username") val username: String,
    @param:Json(name = "email") val email: String,
    @param:Json(name = "points") val points: Int,
    @param:Json(name = "locale") val locale: String,
    @param:Json(name = "avatar") val avatar: String,
    @param:Json(name = "type") val type: String,
    @param:Json(name = "premium") val premium: Int,
    @param:Json(name = "expiration") val expiration: String,
) : Parcelable

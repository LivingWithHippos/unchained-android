package com.github.livingwithhippos.unchained.user.model

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET


@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id")
    val id: Int,
    @Json(name = "username")
    val username: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "points")
    val points: Int,
    @Json(name = "locale")
    val locale: String,
    @Json(name = "avatar")
    val avatar: String,
    @Json(name = "type")
    val type: String,
    @Json(name = "premium")
    val premium: Int,
    @Json(name = "expiration")
    val expiration: String
){

    companion object {
        @JvmStatic
        @BindingAdapter("profileImage")
        fun loadImage(view: ImageView, profileImage: String?) {
            if (profileImage!=null)
                Glide.with(view.context)
                    .load(profileImage)
                    .into(view)
        }
    }
}

interface UserApi {
    @GET("user")
    suspend fun getUser(): Response<User>
}
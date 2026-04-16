package com.github.livingwithhippos.unchained.data.remote

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.utilities.DebridProvider
import com.github.livingwithhippos.unchained.utilities.getDebridProvider
import javax.inject.Inject
import retrofit2.Response

class UserApiHelperImpl
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val userApi: UserApi,
    private val allDebridUserApi: AllDebridUserApi,
) : UserApiHelper {
    override suspend fun getUser(token: String): Response<User> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> userApi.getUser(token)
            DebridProvider.AllDebrid -> {
                val response = allDebridUserApi.getUser(token)
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val user = body?.data?.user
                    if (body?.status == "success" && user != null) {
                        Response.success(user.toUser(System.currentTimeMillis() / 1000))
                    } else allDebridErrorResponse(body?.error)
                }
            }
        }
}

package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.User
import retrofit2.Response

interface UserApiHelper {

    suspend fun getUser(token: String): Response<User>
}
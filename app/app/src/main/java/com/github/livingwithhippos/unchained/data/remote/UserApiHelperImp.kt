package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.User
import retrofit2.Response
import javax.inject.Inject

class UserApiHelperImpl @Inject constructor(private val userApi: UserApi) :
    UserApiHelper {
    override suspend fun getUser(token: String): Response<User> = userApi.getUser(token)
}
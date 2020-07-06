package com.github.livingwithhippos.unchained.user.model

import retrofit2.Response
import javax.inject.Inject

class UserApiHelperImpl @Inject constructor(private val userApi: UserApi) :
    UserApiHelper {
    override suspend fun getUser(token: String): Response<User> = userApi.getUser(token)
}
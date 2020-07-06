package com.github.livingwithhippos.unchained.user.model

import retrofit2.Response
import javax.inject.Inject

class UserApiHelperImpl @Inject constructor(private val userApi: UserApi) :
    UserApiHelper {
    override suspend fun getUser(): Response<User> = userApi.getUser()
}
package com.github.livingwithhippos.unchained.user.model

import retrofit2.Response

interface UserApiHelper {

    suspend fun getUser(): Response<User>
}
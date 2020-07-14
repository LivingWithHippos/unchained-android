package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.user.model.User
import com.github.livingwithhippos.unchained.user.model.UserApiHelper
import javax.inject.Inject


class UserRepository @Inject constructor(private val userApiHelper: UserApiHelper) :
    BaseRepository() {

    suspend fun getUserInfo(token: String): User? {

        val userResponse = safeApiCall(
            call = { userApiHelper.getUser("Bearer $token") },
            errorMessage = "Error Fetching User Info"
        )

        return userResponse;

    }

}
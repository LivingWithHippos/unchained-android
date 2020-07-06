package com.github.livingwithhippos.unchained.user.model

import com.github.livingwithhippos.unchained.base.model.repositories.BaseRepository
import javax.inject.Inject


class UserRepository @Inject constructor(private val userApiHelper: UserApiHelper) : BaseRepository() {

    suspend fun getUserInfo(): User? {

        val userResponse = safeApiCall(
            call = { userApiHelper.getUser() },
            errorMessage = "Error Fetching User Info"
        )

        return userResponse;

    }

}
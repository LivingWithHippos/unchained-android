package com.github.livingwithhippos.unchained.user.model

import com.github.livingwithhippos.unchained.base.model.repository.BaseRepository


class UserRepository(private val api: UserApi) : BaseRepository() {

    suspend fun getUserInfo(): User? {

        val userResponse = safeApiCall(
            call = { api.getUser() },
            errorMessage = "Error Fetching User Info"
        )

        return userResponse;

    }

}
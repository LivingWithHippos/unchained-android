package com.github.livingwithhippos.unchained.newdownload.model

import com.github.livingwithhippos.unchained.authentication.model.UnrestrictApiHelper
import com.github.livingwithhippos.unchained.base.model.repositories.BaseRepository
import javax.inject.Inject

class UnrestrictRepository @Inject constructor(private val unrestrictApiHelper: UnrestrictApiHelper) :
    BaseRepository() {
    suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String? = null,
        remote: Int? = null
    ): UnrestrictedLink? {

        val linkResponse = safeApiCall(
            call = {
                unrestrictApiHelper.getUnrestrictedLink(
                    "Bearer $token",
                    link,
                    password,
                    remote
                )
            },
            errorMessage = "Error Fetching Unrestricted Link Info"
        )

        return linkResponse;
    }
}
package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictApiHelper
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictedLink
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
                    token = "Bearer $token",
                    link = link,
                    password = password,
                    remote = remote
                )
            },
            errorMessage = "Error Fetching Unrestricted Link Info"
        )

        return linkResponse;
    }
}
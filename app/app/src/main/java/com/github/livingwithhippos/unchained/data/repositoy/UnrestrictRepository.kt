package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.remote.UnrestrictApiHelper
import kotlinx.coroutines.delay
import javax.inject.Inject

class UnrestrictRepository @Inject constructor(private val unrestrictApiHelper: UnrestrictApiHelper) :
    BaseRepository() {
    suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String? = null,
        remote: Int? = null
    ): DownloadItem? {

        val linkResponse = unsafeApiCall(
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

        return linkResponse
    }

    suspend fun getUnrestrictedLinkList(
        token: String,
        linksList: List<String>,
        password: String? = null,
        remote: Int? = null
    ): List<DownloadItem?> {
        val unrestrictedLinks = mutableListOf<DownloadItem?>()
        linksList.forEach {
            unrestrictedLinks.add(getUnrestrictedLink(token, it, password, remote))
            // just to be on the safe side...
            delay(100)
        }
        return unrestrictedLinks
    }
}
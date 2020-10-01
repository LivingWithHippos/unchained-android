package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.model.CompleteNetworkResponse
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

        val linkResponse = errorApiResult(
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

        when (linkResponse) {
            is CompleteNetworkResponse.Success -> {
                return linkResponse.data
            }
            is CompleteNetworkResponse.SuccessEmptyBody -> {
                val code = linkResponse.code
                return null
            }
            is CompleteNetworkResponse.RDError<*> -> {
                val error = linkResponse.error
                return null
            }
            is CompleteNetworkResponse.Error -> {
                val message = linkResponse.errorMessage
                return null
            }
            else -> return null
        }
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
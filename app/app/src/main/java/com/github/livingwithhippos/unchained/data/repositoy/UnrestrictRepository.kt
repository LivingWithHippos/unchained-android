package com.github.livingwithhippos.unchained.data.repositoy

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.UnrestrictApiHelper
import kotlinx.coroutines.delay
import javax.inject.Inject

class UnrestrictRepository @Inject constructor(private val unrestrictApiHelper: UnrestrictApiHelper) :
    BaseRepository() {

    suspend fun getEitherUnrestrictedLink(
        token: String,
        link: String,
        password: String? = null,
        remote: Int? = null
    ): Either<UnchainedNetworkException, DownloadItem> {

        val linkResponse = eitherApiResult(
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
    ): List<Either<UnchainedNetworkException, DownloadItem>> {
        val unrestrictedLinks = mutableListOf<Either<UnchainedNetworkException, DownloadItem>>()
        linksList.forEach {
            unrestrictedLinks.add(getEitherUnrestrictedLink(token, it, password, remote))
            // just to be on the safe side...
            delay(100)
        }
        return unrestrictedLinks
    }

    suspend fun getEitherUnrestrictedFolder(
        token: String,
        link: String,
        password: String? = null,
        remote: Int? = null
    ): List<Either<UnchainedNetworkException, DownloadItem>> {

        val folderResponse: Either<UnchainedNetworkException, List<String>> = eitherApiResult(
            call = {
                unrestrictApiHelper.getUnrestrictedFolder(
                    token = "Bearer $token",
                    link = link
                )
            },
            errorMessage = "Error Fetching Unrestricted Folders Info"
        )

        return if (folderResponse is Either.Right) {
            getUnrestrictedLinkList(token, folderResponse.b, password, remote)
        } else {
            listOf(Either.left((folderResponse as Either.Left).a))
        }
    }
}
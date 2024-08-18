package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.UnrestrictApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class UnrestrictRepository
@Inject
constructor(
    private val protoStore: ProtoStore,
    private val unrestrictApiHelper: UnrestrictApiHelper
) : BaseRepository(protoStore) {

    suspend fun getEitherUnrestrictedLink(
        link: String,
        password: String? = null,
        remote: Int? = null
    ): EitherResult<UnchainedNetworkException, DownloadItem> {
        val token = getToken()

        val linkResponse =
            eitherApiResult(
                call = {
                    unrestrictApiHelper.getUnrestrictedLink(
                        token = "Bearer $token", link = link, password = password, remote = remote)
                },
                errorMessage = "Error Fetching Unrestricted Link Info")

        return linkResponse
    }

    suspend fun getUnrestrictedLinkList(
        linksList: List<String>,
        password: String? = null,
        remote: Int? = null,
        callDelay: Long = 100
    ): List<EitherResult<UnchainedNetworkException, DownloadItem>> {
        val unrestrictedLinks =
            mutableListOf<EitherResult<UnchainedNetworkException, DownloadItem>>()
        linksList.forEach {
            unrestrictedLinks.add(getEitherUnrestrictedLink(it, password, remote))
            // just to be on the safe side...
            delay(callDelay)
        }
        return unrestrictedLinks
    }

    suspend fun getEitherUnrestrictedFolder(
        link: String,
        password: String? = null,
        remote: Int? = null
    ): List<EitherResult<UnchainedNetworkException, DownloadItem>> {
        val token = getToken()

        val folderResponse: EitherResult<UnchainedNetworkException, List<String>> =
            eitherApiResult(
                call = {
                    unrestrictApiHelper.getUnrestrictedFolder(token = "Bearer $token", link = link)
                },
                errorMessage = "Error Fetching Unrestricted Folders Info")

        return when (folderResponse) {
            is EitherResult.Success ->
                getUnrestrictedLinkList(folderResponse.success, password, remote)
            is EitherResult.Failure -> listOf(EitherResult.Failure(folderResponse.failure))
        }
    }

    suspend fun getEitherFolderLinks(
        link: String
    ): EitherResult<UnchainedNetworkException, List<String>> {
        val token = getToken()

        val folderResponse: EitherResult<UnchainedNetworkException, List<String>> =
            eitherApiResult(
                call = {
                    unrestrictApiHelper.getUnrestrictedFolder(token = "Bearer $token", link = link)
                },
                errorMessage = "Error Fetching Unrestricted Folders Info")

        return folderResponse
    }

    suspend fun uploadContainer(
        container: ByteArray
    ): EitherResult<UnchainedNetworkException, List<String>> {
        val token = getToken()

        val requestBody: RequestBody =
            container.toRequestBody(
                "application/octet-stream".toMediaTypeOrNull(), 0, container.size)

        val uploadResponse =
            eitherApiResult(
                call = {
                    unrestrictApiHelper.uploadContainer(
                        token = "Bearer $token", container = requestBody)
                },
                errorMessage = "Error Uploading Container")

        return uploadResponse
    }

    suspend fun getContainerLinks(link: String): List<String>? {
        val token = getToken()

        val containerResponse =
            safeApiCall(
                call = {
                    unrestrictApiHelper.getContainerLinks(token = "Bearer $token", link = link)
                },
                errorMessage = "Error getting container files")

        return containerResponse
    }
}

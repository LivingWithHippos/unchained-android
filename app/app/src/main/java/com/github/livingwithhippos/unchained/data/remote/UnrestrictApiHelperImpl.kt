package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import javax.inject.Inject
import okhttp3.RequestBody
import retrofit2.Response

class UnrestrictApiHelperImpl @Inject constructor(private val unrestrictApi: UnrestrictApi) :
    UnrestrictApiHelper {
    override suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String?,
        remote: Int?
    ): Response<DownloadItem> = unrestrictApi.getUnrestrictedLink(token, link, password, remote)

    override suspend fun getUnrestrictedFolder(
        token: String,
        link: String
    ): Response<List<String>> = unrestrictApi.getUnrestrictedFolder(token, link)

    override suspend fun uploadContainer(
        token: String,
        container: RequestBody
    ): Response<List<String>> = unrestrictApi.uploadContainer(token, container)

    override suspend fun getContainerLinks(token: String, link: String): Response<List<String>> =
        unrestrictApi.getContainerLinks(token, link)
}

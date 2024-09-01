package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import okhttp3.RequestBody
import retrofit2.Response

interface UnrestrictApiHelper {

    suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String? = null,
        remote: Int? = null,
    ): Response<DownloadItem>

    suspend fun getUnrestrictedFolder(token: String, link: String): Response<List<String>>

    suspend fun uploadContainer(token: String, container: RequestBody): Response<List<String>>

    suspend fun getContainerLinks(token: String, link: String): Response<List<String>>
}

package com.github.livingwithhippos.unchained.newdownload.model

import com.github.livingwithhippos.unchained.lists.model.DownloadItem
import retrofit2.Response

interface UnrestrictApiHelper {

    suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String? = null,
        remote: Int? = null
    ): Response<DownloadItem>
}
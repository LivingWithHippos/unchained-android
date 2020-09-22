package com.github.livingwithhippos.unchained.newdownload.model

import com.github.livingwithhippos.unchained.lists.model.DownloadItem
import retrofit2.Response
import javax.inject.Inject

class UnrestrictApiHelperImpl @Inject constructor(private val unrestrictApi: UnrestrictApi) :
    UnrestrictApiHelper {
    override suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String?,
        remote: Int?
    ): Response<DownloadItem> =
        unrestrictApi.getUnrestrictedLink(token, link, password, remote)

}
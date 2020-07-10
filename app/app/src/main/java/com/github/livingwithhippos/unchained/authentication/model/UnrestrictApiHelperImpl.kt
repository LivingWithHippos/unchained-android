package com.github.livingwithhippos.unchained.authentication.model

import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictApi
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictedLink
import retrofit2.Response
import javax.inject.Inject

class UnrestrictApiHelperImpl @Inject constructor(private val unrestrictApi: UnrestrictApi) :
    UnrestrictApiHelper {
    override suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String?,
        remote: Int?
    ): Response<UnrestrictedLink> =
        unrestrictApi.getUnrestrictedLink(token, link, password, remote)

}
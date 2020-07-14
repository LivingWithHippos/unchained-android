package com.github.livingwithhippos.unchained.newdownload.model

import retrofit2.Response

interface UnrestrictApiHelper {

    suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String? = null,
        remote: Int? = null
    ): Response<UnrestrictedLink>
}
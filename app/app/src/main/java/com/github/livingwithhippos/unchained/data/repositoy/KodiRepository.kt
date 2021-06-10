package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.model.KodiItem
import com.github.livingwithhippos.unchained.data.model.KodiOpenRequest
import com.github.livingwithhippos.unchained.data.model.KodiParams
import com.github.livingwithhippos.unchained.data.remote.KodiApiHelper
import javax.inject.Inject

class KodiRepository @Inject constructor(
    private val kodiApiHelper: KodiApiHelper
) : BaseRepository() {

    suspend fun playUrl(url: String) {
        val kodiResponse = safeApiCall(
            call = { kodiApiHelper.openUrl(
                KodiOpenRequest(
                    params = KodiParams(
                        item = KodiItem(
                            fileUrl = url
                        )
                    )
                )
            ) },
            errorMessage = "Error Sending url to Kodi"
        )

        return kodiResponse
    }
}
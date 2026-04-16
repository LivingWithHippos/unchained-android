package com.github.livingwithhippos.unchained.data.remote

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.utilities.DebridProvider
import com.github.livingwithhippos.unchained.utilities.getDebridProvider
import javax.inject.Inject
import retrofit2.Response

class DownloadApiHelperImpl
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val downloadApi: DownloadApi,
) : DownloadApiHelper {

    override suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int,
        limit: Int,
    ): Response<List<DownloadItem>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> downloadApi.getDownloads(token, offset, page, limit)
            DebridProvider.AllDebrid -> Response.success(emptyList())
        }

    override suspend fun deleteDownload(token: String, id: String) =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> downloadApi.deleteDownload(token, id)
            DebridProvider.AllDebrid -> Response.success(Unit)
        }
}

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
    private val allDebridUserLinksApi: AllDebridUserLinksApi,
) : DownloadApiHelper {

    override suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int,
        limit: Int,
    ): Response<List<DownloadItem>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> downloadApi.getDownloads(token, offset, page, limit)
            DebridProvider.AllDebrid -> {
                // AllDebrid history returns all links at once; only fetch on first page
                if (page > 1) return Response.success(emptyList())
                val response = allDebridUserLinksApi.getUserHistory(token)
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val links = body?.data?.links
                    if (body?.status == "success" && links != null) {
                        Response.success(links.map { it.toDownloadItem() })
                    } else allDebridErrorResponse(body?.error)
                }
            }
        }

    override suspend fun deleteDownload(token: String, id: String) =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> downloadApi.deleteDownload(token, id)
            // id is the original link URL for AllDebrid history items
            DebridProvider.AllDebrid -> {
                val response = allDebridUserLinksApi.deleteLinks(token, listOf(id))
                if (response.isSuccessful) Response.success(Unit)
                else allDebridErrorResponse(code = response.code(), error = response.body()?.error)
            }
        }
}

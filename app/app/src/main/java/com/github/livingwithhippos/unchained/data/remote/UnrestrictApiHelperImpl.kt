package com.github.livingwithhippos.unchained.data.remote

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.utilities.DebridProvider
import com.github.livingwithhippos.unchained.utilities.getDebridProvider
import javax.inject.Inject
import okhttp3.RequestBody
import retrofit2.Response

class UnrestrictApiHelperImpl
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val unrestrictApi: UnrestrictApi,
    private val allDebridUnrestrictApi: AllDebridUnrestrictApi,
    private val allDebridUserLinksApi: AllDebridUserLinksApi,
) : UnrestrictApiHelper {
    override suspend fun getUnrestrictedLink(
        token: String,
        link: String,
        password: String?,
        remote: Int?,
    ): Response<DownloadItem> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid ->
                unrestrictApi.getUnrestrictedLink(token, link, password, remote)
            DebridProvider.AllDebrid -> {
                val response = allDebridUnrestrictApi.unlockLink(token, link, password)
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val data = body?.data
                    if (body?.status == "success" && data != null) {
                        // Auto-save so the link appears in the downloads/history list.
                        // Failure here is intentionally ignored: the unlock has already succeeded,
                        // and the link would simply not appear in the saved list until refreshed.
                        allDebridUserLinksApi.saveLinks(token, listOf(link))
                        Response.success(data.toDownloadItem(link))
                    } else allDebridErrorResponse(body?.error)
                }
            }
        }

    override suspend fun getUnrestrictedFolder(
        token: String,
        link: String,
    ): Response<List<String>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> unrestrictApi.getUnrestrictedFolder(token, link)
            DebridProvider.AllDebrid -> {
                val response = allDebridUnrestrictApi.getRedirectorLinks(token, listOf(link))
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val data = body?.data
                    if (body?.status == "success" && data != null) {
                        Response.success(data.toLinkList())
                    } else allDebridErrorResponse(body?.error)
                }
            }
        }

    override suspend fun uploadContainer(
        token: String,
        container: RequestBody,
    ): Response<List<String>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> unrestrictApi.uploadContainer(token, container)
            DebridProvider.AllDebrid -> Response.success(emptyList())
        }

    override suspend fun getContainerLinks(token: String, link: String): Response<List<String>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> unrestrictApi.getContainerLinks(token, link)
            DebridProvider.AllDebrid -> Response.success(emptyList())
        }
}

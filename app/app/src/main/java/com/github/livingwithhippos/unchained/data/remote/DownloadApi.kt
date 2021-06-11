package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface DownloadApi {

    /**
     * Get user downloads list
     * You can not use both offset and page at the same time, page is prioritized in case it happens.
     * @param token the authorization token, formed as "Bearer api_token"
     * @param offset Starting offset (must be within 0 and X-Total-Count HTTP header)
     * @param page Page number
     * @param limit Entries returned per page / request (must be within 0 and 100, default: 50)
     * @return a Response<List<DownloadItem>> a list of download items
     */
    @GET("downloads")
    suspend fun getDownloads(
        @Header("Authorization") token: String,
        @Query("offset") offset: Int?,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int = 50
    ): Response<List<DownloadItem>>

    /**
     * Delete a download.
     * @param token the authorization token, formed as "Bearer api_token"
     * @param id the download ID, returned by getDownloads
     */
    @DELETE("downloads/delete/{id}")
    suspend fun deleteDownload(
        @Header("Authorization") token: String,
        @Path("id") id: String,
    ): Response<Unit>
}

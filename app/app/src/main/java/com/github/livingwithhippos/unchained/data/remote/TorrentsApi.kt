package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.AvailableHost
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * This interface is used by Retrofit to manage all the REST calls to the torrents endpoints
 */
interface TorrentsApi {

    @GET("torrents/availableHosts")
    suspend fun getAvailableHosts(
        @Header("Authorization") token: String
    ): Response<List<AvailableHost>>

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<TorrentItem>

    @PUT("torrents/addTorrent")
    suspend fun addTorrent(
        @Header("Authorization") token: String,
        @Body binaryTorrent: RequestBody,
        @Query("host") host: String
    ): Response<UploadedTorrent>

    @FormUrlEncoded
    @POST("torrents/addMagnet")
    suspend fun addMagnet(
        @Header("Authorization") token: String,
        @Field("magnet") magnet: String,
        @Field("host") host: String
    ): Response<UploadedTorrent>

    /**
     * Get a list of the user's torrents.
     * @param token the authorization token, formed as "Bearer api_token"
     * @param offset Starting offset (must be within 0 and X-Total-Count HTTP header)
     * @param page Page number
     * @param limit Entries returned per page / request (must be within 0 and 100, default: 50)
     * @param filter "active", list active torrents first
     * @return a Response<List<TorrentItem>> a list of torrent items
     */
    @GET("torrents")
    suspend fun getTorrentsList(
        @Header("Authorization") token: String,
        @Query("offset") offset: Int? = 0,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 10,
        @Query("filter") filter: String?
    ): Response<List<TorrentItem>>

    /**
     * Select files of a torrent. Required to start a torrent.
     * @param token the authorization token, formed as "Bearer api_token"
     * @param id the torrent ID, returned by addTorrent or getTorrentsList
     * @param files Selected files IDs (comma separated) or "all"
     */
    @FormUrlEncoded
    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Field("files") files: String = "all"
    ): Response<Unit>

    /**
     * Delete a torrent.
     * @param token the authorization token, formed as "Bearer api_token"
     * @param id the torrent ID, returned by addTorrent or getTorrentsList
     */
    @DELETE("torrents/delete/{id}")
    fun deleteTorrent(
        @Header("Authorization") token: String,
        @Path("id") id: String,
    ): Call<ResponseBody>

}
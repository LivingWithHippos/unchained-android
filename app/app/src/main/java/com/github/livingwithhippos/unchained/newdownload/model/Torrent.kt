package com.github.livingwithhippos.unchained.newdownload.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class UploadedTorrent(
    @Json(name = "id")
    val id: String,
    @Json(name = "uri")
    val uri: String
)

@JsonClass(generateAdapter = true)
data class AvailableHost(
    @Json(name = "host")
    val host: String,
    @Json(name = "max_file_size")
    val maxFileSize: Int
)

interface TorrentsApi {

    @GET("torrents/availableHosts")
    suspend fun getAvailableHosts(
        @Header("Authorization") token: String
    ): Response<List<AvailableHost>>

    @PUT("torrents/addTorrent")
    suspend fun addTorrent(
        @Header("Authorization") token: String,
        @Body binaryTorrent: RequestBody,
        @Query("host") host: String
    ): Response<UploadedTorrent>

}
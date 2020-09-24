package com.github.livingwithhippos.unchained.newdownload.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

/*
[
{
    "id": "string",
    "filename": "string",
    "original_filename": "string", // Original name of the torrent
    "hash": "string", // SHA1 Hash of the torrent
    "bytes": int, // Size of selected files only
    "original_bytes": int, // Total size of the torrent
    "host": "string", // Host main domain
    "split": int, // Split size of links
    "progress": int, // Possible values: 0 to 100
    "status": "downloaded", // Current status of the torrent: magnet_error, magnet_conversion, waiting_files_selection, queued, downloading, downloaded, error, virus, compressing, uploading, dead
    "added": "string", // jsonDate
    "files": [
    {
        "id": int,
        "path": "string", // Path to the file inside the torrent, starting with "/"
        "bytes": int,
        "selected": int // 0 or 1
    },
    {
        "id": int,
        "path": "string", // Path to the file inside the torrent, starting with "/"
        "bytes": int,
        "selected": int // 0 or 1
    }
    ],
    "links": [
    "string" // Host URL
    ],
    "ended": "string", // !! Only present when finished, jsonDate
    "speed": int, // !! Only present in "downloading", "compressing", "uploading" status
    "seeders": int // !! Only present in "downloading", "magnet_conversion" status
}
]
*/

@JsonClass(generateAdapter = true)
data class TorrentItem(
    @Json(name = "id")
    val id: String,
    @Json(name = "filename")
    val filename: String,
    @Json(name = "original_filename")
    val originalFilename: String?,
    @Json(name = "hash")
    val hash: String,
    @Json(name = "bytes")
    val bytes: Long,
    @Json(name = "original_bytes")
    val originalBytes: Long?,
    @Json(name = "host")
    val host: String,
    @Json(name = "split")
    val split: Int,
    @Json(name = "progress")
    val progress: Int,
    @Json(name = "status")
    val status: String,
    @Json(name = "added")
    val added: String,
    @Json(name = "files")
    val files: List<InnerTorrentFile>?,
    @Json(name = "links")
    val links: List<String>,
    @Json(name = "ended")
    val ended: String?,
    @Json(name = "speed")
    val speed: Int?,
    @Json(name = "seeders")
    val seeders: Int?
)

@JsonClass(generateAdapter = true)
data class InnerTorrentFile(
    @Json(name = "id")
    val id: Int,
    @Json(name = "path")
    val path: String,
    @Json(name = "bytes")
    val bytes: Long,
    @Json(name = "selected")
    val selected: Int
)

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
        @Query("filter ") filter: String?
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

}
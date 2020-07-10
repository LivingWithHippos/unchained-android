package com.github.livingwithhippos.unchained.newdownload.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

/*
{
    "id": "string",
    "filename": "string",
    "mimeType": "string", // Mime Type of the file, guessed by the file extension
    "filesize": int, // Filesize in bytes, 0 if unknown
    "link": "string", // Original link
    "host": "string", // Host main domain
    "chunks": int, // Max Chunks allowed
    "crc": int, // Disable / enable CRC check 
    "download": "string", // Generated link
    "streamable": int // Is the file streamable on website
}
 */

@JsonClass(generateAdapter = true)
data class UnrestrictedLink(
    @Json(name = "id")
    val id: String,
    @Json(name = "filename")
    val filename: String,
    @Json(name = "mimeType")
    val mimeType: String,
    @Json(name = "filesize")
    val filesize: String,
    @Json(name = "link")
    val link: String,
    @Json(name = "host")
    val host: String,
    @Json(name = "chunks")
    val chunks: String,
    @Json(name = "crc")
    val crc: String,
    @Json(name = "download")
    val download: String,
    @Json(name = "streamable")
    val streamable: String
)

interface UnrestrictApi {

    /**
     * Unrestrict a hoster link and get a new unrestricted link
     *
     */
    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun getUnrestrictedLink(
        @Header("Authorization") token: String,
        // The original hoster link
        @Field("link ") link: String,
        // Password to unlock the file access hoster side
        @Field("link ") password: String? = null,
        // 0 or 1, use Remote traffic, dedicated servers and account sharing protections lifted
        @Field("remote ") remote: Int? = null
    ): Response<UnrestrictedLink>

}
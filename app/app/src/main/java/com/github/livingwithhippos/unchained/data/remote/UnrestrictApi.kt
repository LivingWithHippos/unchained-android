package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

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

interface UnrestrictApi {

    /**
     * Unrestrict a hoster link and get a new download link
     *
     * @param token the authentication token
     * @param link the original hoster link
     * @param password password to unlock the file access hoster side
     * @param remote 0 or 1, use Remote traffic, dedicated servers and account sharing protections
     *   lifted
     * @return a [DownloadItem] representing the download
     */
    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun getUnrestrictedLink(
        @Header("Authorization") token: String,
        @Field("link") link: String,
        @Field("password") password: String? = null,
        @Field("remote") remote: Int? = null
    ): Response<DownloadItem>

    /**
     * Unrestrict a folder link and get a new list of link
     *
     * @param token the authentication token
     * @param link the original folder link
     * @return
     */
    @FormUrlEncoded
    @POST("unrestrict/folder")
    suspend fun getUnrestrictedFolder(
        @Header("Authorization") token: String,
        @Field("link") link: String
    ): Response<List<String>>

    /**
     * Upload a container
     *
     * @param token the authentication token
     * @param container the container file in binary form
     * @return the list of the links in the container. Not unrestricted.
     */
    @PUT("unrestrict/containerFile")
    suspend fun uploadContainer(
        @Header("Authorization") token: String,
        @Body container: RequestBody
    ): Response<List<String>>

    /**
     * Unrestrict a folder link and get a new list of link
     *
     * @param token the authentication token
     * @param link the http link to the container file
     * @return the list of the links in the container. Not unrestricted.
     */
    @FormUrlEncoded
    @POST("unrestrict/folder")
    suspend fun getContainerLinks(
        @Header("Authorization") token: String,
        @Field("link") link: String
    ): Response<List<String>>
}

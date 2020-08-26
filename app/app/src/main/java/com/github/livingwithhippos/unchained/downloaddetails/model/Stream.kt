package com.github.livingwithhippos.unchained.downloaddetails.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class Stream(
    @Json(name = "apple")
    val apple: Quality,
    @Json(name = "dash")
    val dash: Quality,
    @Json(name = "liveMP4")
    val liveMP4: Quality,
    @Json(name = "h264WebM")
    val h264WebM: Quality
)

@JsonClass(generateAdapter = true)
data class Quality(
    @Json(name = "full")
    val link: String
)

/**
 * The APIs in this interface will not work with an open source token.
 */
interface StreamingApi {

    @GET("streaming/transcode/{id}")
    suspend fun getStreams(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Stream>
}